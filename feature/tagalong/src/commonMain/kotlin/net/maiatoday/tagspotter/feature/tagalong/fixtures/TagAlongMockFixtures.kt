package net.maiatoday.tagspotter.feature.tagalong.fixtures

object TagAlongMockFixtures {

    val SPOT_DOSSIER_JSON = """
        {
          "type": "spot_card",
          "data": {
            "spotId": "spot-berlin-teufelsberg-01",
            "title": "Cosmic Chameleon",
            "artist": "AeroFlux",
            "imageUrl": "https://picsum.photos/seed/teufelsberg/800/500",
            "tags": ["stencil", "glow", "berlin-underground", "mural"],
            "distanceMeters": 180.0,
            "latitude": 52.4972,
            "longitude": 13.2415,
            "isStarred": false
          }
        }
    """.trimIndent()

    val TOUR_SAMPLE_JSON = """
        {
          "type": "tour_card",
          "tourId": "tour-mitte-hidden-gems",
          "tourTitle": "Mitte Hidden Alleyways Tour",
          "description": "Discover 4 underground street art murals tucked away in Berlin Mitte courtyards.",
          "estimatedMinutes": 45,
          "totalDistanceKm": 2.1,
          "waypoints": [
            {
              "index": 1,
              "title": "The Cyber Samurai",
              "address": "Rosenthaler Str. 39",
              "latitude": 52.5251,
              "longitude": 13.4021,
              "completed": true
            },
            {
              "index": 2,
              "title": "Neon Falcon",
              "address": "Linienstr. 142",
              "latitude": 52.5283,
              "longitude": 13.3980,
              "completed": false
            },
            {
              "index": 3,
              "title": "Quantum Drip Wall",
              "address": "Auguststr. 25",
              "latitude": 52.5270,
              "longitude": 13.3942,
              "completed": false
            },
            {
              "index": 4,
              "title": "Analog Dreamscape",
              "address": "Tucholskystr. 18",
              "latitude": 52.5245,
              "longitude": 13.3918,
              "completed": false
            }
          ]
        }
    """.trimIndent()

    val ARTIST_BIO_JSON = """
        {
          "type": "artist_bio_card",
          "data": {
            "artistName": "Kira 'Volt' Jensen",
            "avatarUrl": "https://picsum.photos/seed/volt/200/200",
            "bio": "Pioneering Berlin street artist combining UV-reactive spray pigments with augmented reality stencils since 2018.",
            "activeYears": "2018 - Present",
            "styles": ["UV-Reactive", "Cyberpunk", "Large Scale Murals", "Stencil"],
            "artworks": [
              {
                "id": "spot-volt-01",
                "title": "Subway Sentinel",
                "imageUrl": "https://picsum.photos/seed/volt1/300/300"
              },
              {
                "id": "spot-volt-02",
                "title": "Digital Aurora",
                "imageUrl": "https://picsum.photos/seed/volt2/300/300"
              },
              {
                "id": "spot-volt-03",
                "title": "Ghost in the Spree",
                "imageUrl": "https://picsum.photos/seed/volt3/300/300"
              }
            ]
          }
        }
    """.trimIndent()

    val QUIZ_SAMPLE_JSON = """
        {
          "type": "style_quiz",
          "quizId": "quiz-berlin-styles-01",
          "question": "Which visual hallmark identifies this artwork as a 'Wheatpaste' rather than a pure stencil?",
          "imageUrl": "https://picsum.photos/seed/pasteup/600/350",
          "options": [
            {
              "id": "opt-1",
              "text": "Paper wrinkling and layered adhesion edges",
              "isCorrect": true
            },
            {
              "id": "opt-2",
              "text": "Overspray halo and crisp masking tape marks",
              "isCorrect": false
            },
            {
              "id": "opt-3",
              "text": "High-gloss protective resin varnish",
              "isCorrect": false
            }
          ],
          "explanation": "Wheatpaste (paste-up) art uses paper prints glued to walls with starch adhesive, creating visible seam crinkles and delicate texture!"
        }
    """.trimIndent()

    val VOTING_SAMPLE_JSON = """
        {
          "type": "community_voting",
          "pollId": "poll-wall-curation-01",
          "title": "Community Wall Preservation",
          "description": "Should the RAW-Gelände 'Phoenix Wall' be sealed with anti-graffiti clearcoat to preserve it for 2 years, or kept open for spontaneous fresh tags?",
          "options": [
            {
              "id": "preserve",
              "label": "Preserve Phoenix Wall (Seal it)",
              "votes": 142
            },
            {
              "id": "keep-open",
              "label": "Keep it Open (Freedom to repaint)",
              "votes": 98
            },
            {
              "id": "community-vote",
              "label": "Host a local artist jam instead",
              "votes": 67
            }
          ],
          "totalVotes": 307
        }
    """.trimIndent()

    val MAP_OVERLAY_JSON = """
        {
          "type": "map_pin_overlay",
          "title": "Kreuzberg Neon Cluster",
          "centerLatitude": 52.4990,
          "centerLongitude": 13.4180,
          "zoomLevel": 15.0,
          "pins": [
            {
              "id": "pin-kbg-01",
              "title": "Astronaut Cosmonaut (Victor Ash)",
              "latitude": 52.5015,
              "longitude": 13.4190,
              "category": "Mural"
            },
            {
              "id": "pin-kbg-02",
              "title": "Yellow Man (Os Gemeos)",
              "latitude": 52.4988,
              "longitude": 13.4215,
              "category": "Mural"
            },
            {
              "id": "pin-kbg-03",
              "title": "The Pink Man (Blu)",
              "latitude": 52.4975,
              "longitude": 13.4240,
              "category": "Piece"
            }
          ]
        }
    """.trimIndent()

    val MALFORMED_JSON = """
        {
          "type": "alien_teleport_widget",
          "data": {
            "flux_capacitor": true,
            "target_galaxy": "Andromeda"
          }
        }
    """.trimIndent()

    val ALL_FIXTURES: Map<String, String> = mapOf(
        "Spot Card" to SPOT_DOSSIER_JSON,
        "Walking Tour" to TOUR_SAMPLE_JSON,
        "Artist Bio" to ARTIST_BIO_JSON,
        "Style Quiz" to QUIZ_SAMPLE_JSON,
        "Community Vote" to VOTING_SAMPLE_JSON,
        "Map Overlay" to MAP_OVERLAY_JSON,
        "Malformed (Fallback)" to MALFORMED_JSON
    )
}
