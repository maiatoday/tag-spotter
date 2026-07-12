import Foundation
import FirebaseVertexAI
import SharedApp
import UIKit

class SwiftFirebaseAiService: NSObject, AiRecognitionService {
    var isSupported: Bool {
        return true
    }
    
    private lazy var model = VertexAI.vertexAI().generativeModel(modelName: "gemini-2.5-flash")
    
    func identifyArtist(
        imagePath: String,
        category: String,
        currentArtist: String?,
        currentTitle: String?,
        thumbnailPath: String?
    ) async throws -> AiSuggestion? {
        let fileURL: URL
        if imagePath.hasPrefix("file://") {
            guard let url = URL(string: imagePath) else {
                print("SwiftFirebaseAiService: Invalid image URL \(imagePath)")
                return nil
            }
            fileURL = url
        } else {
            fileURL = URL(fileURLWithPath: imagePath)
        }
        
        guard let data = try? Data(contentsOf: fileURL),
              let uiImage = UIImage(data: data) else {
            print("SwiftFirebaseAiService: Failed to load image from path \(imagePath)")
            return nil
        }
        
        var prompt = "You are an expert art identifier. Analyze this image. This is a work of \(category)."
        if let artist = currentArtist, !artist.isEmpty {
            prompt += " The current artist is suspected to be \(artist)."
        }
        if let title = currentTitle, !title.isEmpty {
            prompt += " The current title is suspected to be \(title)."
        }
        prompt += " Please identify the artist, the title of the artwork, and suggest a list of relevant tags (style, medium, themes, colors, up to 5 tags). Provide your answer in raw JSON format matching this schema: {\"artist\": \"...\", \"title\": \"...\", \"tags\": [\"tag1\", \"tag2\", ...]}"
        
        do {
            let response = try await model.generateContent(uiImage, prompt)
            guard let text = response.text else { return nil }
            
            var cleanText = text.trimmingCharacters(in: .whitespacesAndNewlines)
            if cleanText.hasPrefix("```") {
                if let firstLineEnd = cleanText.firstIndex(of: "\n") {
                    cleanText = String(cleanText[firstLineEnd...])
                }
                if cleanText.hasSuffix("```") {
                    cleanText = String(cleanText.dropLast(3))
                }
                cleanText = cleanText.trimmingCharacters(in: .whitespacesAndNewlines)
            }
            
            struct TempSuggestion: Codable {
                let artist: String?
                let title: String?
                let tags: [String]?
            }
            
            guard let jsonData = cleanText.data(using: .utf8),
                  let temp = try? JSONDecoder().decode(TempSuggestion.self, from: jsonData) else {
                print("SwiftFirebaseAiService: Failed to parse model response: \(text)")
                return nil
            }
            
            return AiSuggestion(artist: temp.artist, title: temp.title, tags: temp.tags ?? [])
        } catch {
            print("SwiftFirebaseAiService error: \(error)")
            throw error
        }
    }
    
    func searchWikipediaForSpot(
        title: String,
        category: String,
        artists: [String]
    ) async throws -> String? {
        let artistList = artists.joined(separator: ", ")
        let prompt = "Provide a brief summary and search information about the artwork '\(title)' (\(category)) by \(artistList). Find relevant historical background or Wikipedia-style info. Keep it short (1-2 paragraphs)."
        
        do {
            let response = try await model.generateContent(prompt)
            return response.text
        } catch {
            print("SwiftFirebaseAiService search error: \(error)")
            throw error
        }
    }
}
