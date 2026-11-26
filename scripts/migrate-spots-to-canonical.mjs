#!/usr/bin/env node
/**
 * One-time migration script:
 * Copies user spots from `/users/{uid}/spots/{uuid}` into the canonical `/spots/{uuid}`
 * collection with top-level `ownerUid` and `isPublic` fields, skipping any imported pack spots.
 *
 * Writes use atomic `create()` (create-if-absent) so retries never overwrite an already-migrated
 * or newer canonical document. Legacy `/public_spots/{id}` documents are deleted only when a
 * canonical `/spots/{id}` replacement exists with the same `ownerUid`.
 *
 * Usage (dry run by default):
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json \
 *     node scripts/migrate-spots-to-canonical.mjs
 *
 * To execute writes:
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json \
 *     node scripts/migrate-spots-to-canonical.mjs --execute
 *
 * To also delete verified legacy `/public_spots` documents after migrating:
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json \
 *     node scripts/migrate-spots-to-canonical.mjs --execute --delete-legacy-public
 */

import { initializeApp, applicationDefault } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const args = new Set(process.argv.slice(2));
const execute = args.has("--execute");
const deleteLegacyPublic = args.has("--delete-legacy-public");

initializeApp({ credential: applicationDefault() });
const db = getFirestore();

async function main() {
  console.log(`Mode: ${execute ? "EXECUTE" : "DRY RUN (no writes)"}`);

  const userRefs = await db.collection("users").listDocuments();
  console.log(`Found ${userRefs.length} user document(s).`);

  let migrated = 0;
  let skippedExisting = 0;
  let skippedImported = 0;

  for (const userRef of userRefs) {
    const uid = userRef.id;
    const spotsSnap = await userRef.collection("spots").get();
    console.log(`User ${uid}: ${spotsSnap.size} spot(s)`);

    for (const doc of spotsSnap.docs) {
      const data = doc.data();
      const spot = data.spot || {};
      const isImported = Boolean(spot.isImported) || Boolean(spot.parentPackId);

      if (isImported) {
        skippedImported++;
        console.log(`  [SKIP imported] ${doc.id}`);
        continue;
      }

      const isPublic = Boolean(spot.isPublic);
      const canonicalDoc = {
        ownerUid: uid,
        isPublic,
        spot: {
          ...spot,
          ownerUid: uid,
          isPublic,
          origin: "OWN",
          isImported: false,
        },
        images: (data.images || []).map((img) => ({
          ...img,
          remoteThumbnailUrl:
            img.remoteThumbnailUrl ||
            (typeof img.thumbnailPath === "string" && img.thumbnailPath.startsWith("http")
              ? img.thumbnailPath
              : ""),
        })),
        notes: data.notes || [],
      };

      const targetRef = db.collection("spots").doc(doc.id);
      if (execute) {
        try {
          // Atomic create-if-absent so retries never overwrite newer canonical data
          await targetRef.create(canonicalDoc);
          migrated++;
          console.log(`  [MIGRATED] /spots/${doc.id} (ownerUid=${uid}, isPublic=${isPublic})`);
        } catch (err) {
          if (err && (err.code === 6 || err.code === "already-exists")) {
            skippedExisting++;
            console.log(`  [SKIP exists] /spots/${doc.id} already exists`);
          } else {
            throw err;
          }
        }
      } else {
        const existingSnap = await targetRef.get();
        if (existingSnap.exists) {
          skippedExisting++;
          console.log(`  [WOULD SKIP exists] /spots/${doc.id} already exists`);
        } else {
          migrated++;
          console.log(`  [WOULD MIGRATE] /spots/${doc.id} (ownerUid=${uid}, isPublic=${isPublic})`);
        }
      }
    }
  }

  if (deleteLegacyPublic) {
    const publicSnap = await db.collection("public_spots").get();
    console.log(`Legacy /public_spots documents: ${publicSnap.size}`);
    let deletedLegacy = 0;
    let retainedLegacy = 0;

    for (const doc of publicSnap.docs) {
      const legacyData = doc.data();
      const legacyOwnerUid = legacyData?.spot?.ownerUid || legacyData?.ownerUid || null;
      const canonicalSnap = await db.collection("spots").doc(doc.id).get();
      const canonicalData = canonicalSnap.exists ? canonicalSnap.data() : null;

      const verifiedReplacement =
        canonicalSnap.exists &&
        canonicalData != null &&
        (!legacyOwnerUid || canonicalData.ownerUid === legacyOwnerUid);

      if (!verifiedReplacement) {
        retainedLegacy++;
        console.warn(
          `  [RETAIN legacy] /public_spots/${doc.id} — no verified canonical /spots/${doc.id} replacement (legacyOwner=${legacyOwnerUid}, canonicalOwner=${canonicalData?.ownerUid ?? "missing"})`
        );
        continue;
      }

      if (execute) {
        await doc.ref.delete();
      }
      deletedLegacy++;
      console.log(`  [${execute ? "DELETED" : "WOULD DELETE"}] /public_spots/${doc.id}`);
    }
    console.log(`Legacy public cleanup: deleted=${deletedLegacy}, retained=${retainedLegacy}.`);
  }

  console.log(`Done. Migrated=${migrated}, SkippedExisting=${skippedExisting}, SkippedImported=${skippedImported}.`);
}

main().catch((err) => {
  console.error("Migration failed:", err);
  process.exit(1);
});
