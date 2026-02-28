
import json
import os

try:
    with open('temp_style.json', 'r', encoding='utf-8') as f:
        style = json.load(f)

    # Token from user's original style
    token = "eyJhbGciOiJIUzI1NiJ9.eyJhIjoiYWNfbXE5aGY0NTAiLCJqdGkiOiJmZTdjMGNiMiJ9.-bvlFsPKLZPUMwBof2cfxWOSgEVDEgNknNpE3LJLlZc"

    # The source in temp_style.json is defined as:
    # "sources": { "carto": { "type": "vector", "url": "https://tiles.basemaps.cartocdn.com/vector/carto.streets/v1/tiles.json" } }
    
    if 'sources' in style and 'carto' in style['sources']:
        carto_source = style['sources']['carto']
        if 'url' in carto_source:
            url = carto_source['url']
            if 'auth_token' not in url:
                if '?' in url:
                    carto_source['url'] = url + "&auth_token=" + token
                else:
                    carto_source['url'] = url + "?auth_token=" + token

    # Create the 3D building layer
    building_layer = {
        "id": "3d-buildings",
        "type": "fill-extrusion",
        "source": "carto",
        "source-layer": "building",
        "minzoom": 15,
        "paint": {
            "fill-extrusion-color": "#aaa",
            "fill-extrusion-height": ["get", "render_height"],
            "fill-extrusion-base": ["get", "render_min_height"],
            "fill-extrusion-opacity": 0.6
        }
    }

    # Find where to insert it (before labels usually, or just append)
    # Appending at the end might cover labels if they are not symbols.
    # Usually buildings should be below labels.
    # Let's try to find the first symbol layer and insert before it.
    insert_idx = len(style['layers'])
    for i, layer in enumerate(style['layers']):
        if layer['type'] == 'symbol':
            insert_idx = i
            break
            
    style['layers'].insert(insert_idx, building_layer)

    with open('app/src/main/assets/style_3d.json', 'w', encoding='utf-8') as f:
        json.dump(style, f, indent=2)

    print("Created style_3d.json successfully")

except Exception as e:
    print(f"Error: {e}")
