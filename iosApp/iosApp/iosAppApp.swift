import SwiftUI
import SharedApp
import ZIPFoundation
import FirebaseCore
import FirebaseAppCheck

@main
struct iosAppApp: App {
    init() {
        // App Check Provider Setup
        #if DEBUG
        let providerFactory = AppCheckDebugProviderFactory()
        #else
        let providerFactory = AppAttestProviderFactory()
        #endif
        AppCheck.setAppCheckProviderFactory(providerFactory)
        
        FirebaseApp.configure()
        
        let swiftAiService = SwiftFirebaseAiService()
        DiHelperKt.doInitKoin(platformModules: [
            IosSecretsModuleKt.createIosSecretsModule(),
            IosAiModuleKt.createIosAiModule(aiService: swiftAiService)
        ])
        
        IosZipHelper.shared.unzipCallback = { zipFilePath, destDirPath in
            do {
                try FileManager.default.unzipItem(
                    at: URL(fileURLWithPath: zipFilePath),
                    to: URL(fileURLWithPath: destDirPath)
                )
                return true
            } catch {
                print("Extraction failed: \(error)")
                return false
            }
        }
        
        IosZipHelper.shared.zipCallback = { sourceDirPath, destZipPath in
            do {
                try FileManager.default.zipItem(
                    at: URL(fileURLWithPath: sourceDirPath),
                    to: URL(fileURLWithPath: destZipPath)
                )
                return true
            } catch {
                print("Zipping failed: \(error)")
                return false
            }
        }
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
