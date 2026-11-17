package net.maiatoday.tagspotter.feature.tagalong.fixtures

object TagAlongMockFixtures {

    val SPOT_DOSSIER_JSON = """
        [
          {
            "version": "v0.9",
            "createSurface": {
              "surfaceId": "spot-surface-1",
              "catalogId": "net.maiatoday.tagspotter.hybrid"
            }
          },
          {
            "version": "v0.9",
            "updateDataModel": {
              "surfaceId": "spot-surface-1",
              "path": "/spot",
              "value": {
                "title": "Cosmic Chameleon",
                "artist": "AeroFlux",
                "imageUrl": "https://picsum.photos/seed/teufelsberg/800/500",
                "isStarred": false
              }
            }
          },
          {
            "version": "v0.9",
            "updateComponents": {
              "surfaceId": "spot-surface-1",
              "components": [
                {
                  "id": "root",
                  "type": "SpotCard",
                  "properties": {
                    "spotId": "spot-berlin-teufelsberg-01",
                    "title": {"path": "/spot/title"},
                    "artist": {"path": "/spot/artist"},
                    "imageUrl": {"path": "/spot/imageUrl"},
                    "tags": ["stencil", "glow", "berlin-underground", "mural"],
                    "distanceMeters": 180.0,
                    "latitude": 52.4972,
                    "longitude": 13.2415,
                    "isStarred": {"path": "/spot/isStarred"}
                  }
                }
              ]
            }
          }
        ]
    """.trimIndent()

    val TOUR_SAMPLE_JSON = """
        [
          {
            "version": "v0.9",
            "createSurface": {
              "surfaceId": "tour-surface-1",
              "catalogId": "net.maiatoday.tagspotter.hybrid"
            }
          },
          {
            "version": "v0.9",
            "updateDataModel": {
              "surfaceId": "tour-surface-1",
              "path": "/tour",
              "value": {
                "title": "Mitte Hidden Alleyways Tour",
                "description": "Discover 4 underground street art murals tucked away in Berlin Mitte courtyards.",
                "estimatedMinutes": 45,
                "totalDistanceKm": 2.1
              }
            }
          },
          {
            "version": "v0.9",
            "updateComponents": {
              "surfaceId": "tour-surface-1",
              "components": [
                {
                  "id": "root",
                  "type": "TourCard",
                  "properties": {
                    "tourId": "tour-mitte-hidden-gems",
                    "tourTitle": {"path": "/tour/title"},
                    "description": {"path": "/tour/description"},
                    "estimatedMinutes": {"path": "/tour/estimatedMinutes"},
                    "totalDistanceKm": {"path": "/tour/totalDistanceKm"},
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
                }
              ]
            }
          }
        ]
    """.trimIndent()

    val ARTIST_BIO_JSON = """
        [
          {
            "version": "v0.9",
            "createSurface": {
              "surfaceId": "artist-surface-1",
              "catalogId": "net.maiatoday.tagspotter.hybrid"
            }
          },
          {
            "version": "v0.9",
            "updateDataModel": {
              "surfaceId": "artist-surface-1",
              "path": "/artist",
              "value": {
                "artistName": "Kira 'Volt' Jensen",
                "avatarUrl": "https://picsum.photos/seed/volt/200/200",
                "bio": "Pioneering Berlin street artist combining UV-reactive spray pigments with augmented reality stencils since 2018.",
                "activeYears": "2018 - Present"
              }
            }
          },
          {
            "version": "v0.9",
            "updateComponents": {
              "surfaceId": "artist-surface-1",
              "components": [
                {
                  "id": "root",
                  "type": "ArtistBioCard",
                  "properties": {
                    "artistName": {"path": "/artist/artistName"},
                    "avatarUrl": {"path": "/artist/avatarUrl"},
                    "bio": {"path": "/artist/bio"},
                    "activeYears": {"path": "/artist/activeYears"},
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
              ]
            }
          }
        ]
    """.trimIndent()

    val QUIZ_SAMPLE_JSON = """
        [
          {
            "version": "v0.9",
            "createSurface": {
              "surfaceId": "quiz-surface-1",
              "catalogId": "net.maiatoday.tagspotter.hybrid"
            }
          },
          {
            "version": "v0.9",
            "updateComponents": {
              "surfaceId": "quiz-surface-1",
              "components": [
                {
                  "id": "root",
                  "type": "Card",
                  "properties": {
                    "child": "col1"
                  }
                },
                {
                  "id": "col1",
                  "type": "Column",
                  "properties": {
                    "children": ["q_title", "q_text", "picker", "submit_btn"]
                  }
                },
                {
                  "id": "q_title",
                  "type": "Text",
                  "properties": {
                    "text": "Style Quiz: Street Art Techniques",
                    "variant": "H3"
                  }
                },
                {
                  "id": "q_text",
                  "type": "Text",
                  "properties": {
                    "text": "Which visual hallmark identifies this artwork as a 'Wheatpaste' rather than a pure stencil?",
                    "variant": "Body"
                  }
                },
                {
                  "id": "picker",
                  "type": "ChoicePicker",
                  "properties": {
                    "options": [
                      {"id": "opt-1", "label": "Paper wrinkling and layered adhesion edges"},
                      {"id": "opt-2", "label": "Overspray halo and crisp masking tape marks"},
                      {"id": "opt-3", "label": "High-gloss protective resin varnish"}
                    ],
                    "value": {"path": "/selectedOption"}
                  }
                },
                {
                  "id": "submit_btn",
                  "type": "Button",
                  "properties": {
                    "child": "btn_label",
                    "action": {
                      "functionCall": {
                        "call": "clientEvent",
                        "args": {
                          "name": "answer_quiz",
                          "quizId": "quiz-berlin-styles-01",
                          "selectedOption": {"path": "/selectedOption"}
                        }
                      }
                    }
                  }
                },
                {
                  "id": "btn_label",
                  "type": "Text",
                  "properties": {
                    "text": "Submit Answer"
                  }
                }
              ]
            }
          }
        ]
    """.trimIndent()

    val VOTING_SAMPLE_JSON = """
        [
          {
            "version": "v0.9",
            "createSurface": {
              "surfaceId": "voting-surface-1",
              "catalogId": "net.maiatoday.tagspotter.hybrid"
            }
          },
          {
            "version": "v0.9",
            "updateComponents": {
              "surfaceId": "voting-surface-1",
              "components": [
                {
                  "id": "root",
                  "type": "Card",
                  "properties": {
                    "child": "col1"
                  }
                },
                {
                  "id": "col1",
                  "type": "Column",
                  "properties": {
                    "children": ["v_title", "v_desc", "v_picker", "v_btn"]
                  }
                },
                {
                  "id": "v_title",
                  "type": "Text",
                  "properties": {
                    "text": "Community Wall Preservation",
                    "variant": "H3"
                  }
                },
                {
                  "id": "v_desc",
                  "type": "Text",
                  "properties": {
                    "text": "Should the RAW-Gelände 'Phoenix Wall' be sealed with anti-graffiti clearcoat or kept open for fresh tags?",
                    "variant": "Body"
                  }
                },
                {
                  "id": "v_picker",
                  "type": "ChoicePicker",
                  "properties": {
                    "options": [
                      {"id": "preserve", "label": "Preserve Phoenix Wall (Seal it)"},
                      {"id": "keep-open", "label": "Keep it Open (Freedom to repaint)"},
                      {"id": "community-vote", "label": "Host a local artist jam instead"}
                    ],
                    "value": {"path": "/selectedOption"}
                  }
                },
                {
                  "id": "v_btn",
                  "type": "Button",
                  "properties": {
                    "child": "v_btn_label",
                    "action": {
                      "functionCall": {
                        "call": "clientEvent",
                        "args": {
                          "name": "vote_option",
                          "pollId": "poll-wall-curation-01",
                          "selectedOption": {"path": "/selectedOption"}
                        }
                      }
                    }
                  }
                },
                {
                  "id": "v_btn_label",
                  "type": "Text",
                  "properties": {
                    "text": "Vote Now (307 votes)"
                  }
                }
              ]
            }
          }
        ]
    """.trimIndent()

    val MAP_OVERLAY_JSON = """
        [
          {
            "version": "v0.9",
            "createSurface": {
              "surfaceId": "map-surface-1",
              "catalogId": "net.maiatoday.tagspotter.hybrid"
            }
          },
          {
            "version": "v0.9",
            "updateComponents": {
              "surfaceId": "map-surface-1",
              "components": [
                {
                  "id": "root",
                  "type": "MapPinOverlayWidget",
                  "properties": {
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
                }
              ]
            }
          }
        ]
    """.trimIndent()

    val MALFORMED_JSON = """
        [
          {
            "version": "v0.9",
            "createSurface": {
              "surfaceId": "malformed-surface-1",
              "catalogId": "net.maiatoday.tagspotter.hybrid"
            }
          },
          {
            "version": "v0.9",
            "updateComponents": {
              "surfaceId": "malformed-surface-1",
              "components": [
                {
                  "id": "root",
                  "type": "AlienTeleportWidget",
                  "properties": {
                    "flux_capacitor": true
                  }
                }
              ]
            }
          }
        ]
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
