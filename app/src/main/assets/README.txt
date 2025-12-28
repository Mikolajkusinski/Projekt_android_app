Instrukcja trybu offline
=========================

Struktura katalogów (assets):
- models.json — lista modeli (2D i 3D) wyświetlana w aplikacji
- images/2d/ — obrazy 2D (np. SVG/PNG/JPG)
- models/3d/ — modele 3D (GLB/GLTF)
- vendor/model-viewer/ — lokalny skrypt `model-viewer.min.js`

Szybki start:
1) Skopiuj swoje pliki do odpowiednich folderów, np.:
   - images/2d/logo.svg
   - models/3d/helmet.glb
2) Zaktualizuj `models.json`, np.:
```
[
  {"name":"Logo (SVG)", "type":"2d", "path":"images/2d/logo.svg", "format":"svg", "createdAt":"2025-01-01"},
  {"name":"Katalog (PNG)", "type":"2d", "path":"images/2d/catalog.png", "format":"png", "createdAt":"2025-01-15"},
  {"name":"Model 3D (GLB)", "type":"3d", "path":"models/3d/helmet.glb", "format":"glb", "createdAt":"2025-01-20"}
]
```
3) Podgląd 3D offline wymaga lokalnego skryptu `vendor/model-viewer/model-viewer.min.js`.
   - Pobierz `@google/model-viewer` i skopiuj minifikowany plik tutaj (zastąp placeholder).

Uwagi:
- Ścieżki w `models.json` są RELATYWNE do katalogu assets (aplikacja łączy je do `file:///android_asset/...`).
- Dla GLTF z zewnętrznymi plikami (BIN, tekstury) zalecamy konwersję do GLB lub poprawne ścieżki względem assets.
- Jeśli plik nie istnieje, pozycja może nie wyświetlić się poprawnie.
