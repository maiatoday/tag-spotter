import SwiftUI
import SharedApp
import ZIPFoundation

@main
struct iosAppApp: App {
    init() {
        DiHelperKt.doInitKoin(platformModules: [
            IosSecretsModuleKt.createIosSecretsModule()
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
