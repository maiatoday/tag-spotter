import SwiftUI
import SharedApp

@main
struct iosAppApp: App {
    init() {
        DiHelperKt.doInitKoin(platformModules: [
            IosSecretsModuleKt.createIosSecretsModule()
        ])
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
