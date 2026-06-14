import SwiftUI
import SharedApp
import PhotosUI
import UniformTypeIdentifiers

struct ComposeView: UIViewControllerRepresentable {
    var onTriggerFiles: (@escaping (String) -> KotlinUnit) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.createMainViewController(onTriggerFiles: onTriggerFiles)
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ImagePicker: UIViewControllerRepresentable {
    @Binding var isPresented: Bool
    var onPicked: (String) -> Void

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var config = PHPickerConfiguration()
        config.filter = .images
        config.selectionLimit = 1
        
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let parent: ImagePicker

        init(_ parent: ImagePicker) {
            self.parent = parent
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            parent.isPresented = false
            
            guard let provider = results.first?.itemProvider else { return }
            
            provider.loadFileRepresentation(forTypeIdentifier: UTType.image.identifier) { url, error in
                if let url = url {
                    let tempDir = FileManager.default.temporaryDirectory
                    let destUrl = tempDir.appendingPathComponent(UUID().uuidString + "." + url.pathExtension)
                    do {
                        if FileManager.default.fileExists(atPath: destUrl.path) {
                            try FileManager.default.removeItem(at: destUrl)
                        }
                        try FileManager.default.copyItem(at: url, to: destUrl)
                        DispatchQueue.main.async {
                            self.parent.onPicked(destUrl.absoluteString)
                        }
                    } catch {
                        print("Error copying picked image: \(error)")
                    }
                }
            }
        }
    }
}

struct ContentView: View {
    @State private var isPickerPresented = false
    @State private var onPickedCallback: ((String) -> KotlinUnit)? = nil

    var body: some View {
        ZStack {
            ComposeView(onTriggerFiles: { callback in
                self.onPickedCallback = callback
                self.isPickerPresented = true
            })
            .ignoresSafeArea(.all, edges: .all)
        }
        .sheet(isPresented: $isPickerPresented) {
            ImagePicker(isPresented: $isPickerPresented) { urlString in
                let _ = self.onPickedCallback?(urlString)
            }
        }
    }
}
