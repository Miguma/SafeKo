
const https = require('https');
const fs = require('fs');

const url = 'https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json';
const token = "eyJhbGciOiJIUzI1NiJ9.eyJhIjoiYWNfbXE5aGY0NTAiLCJqdGkiOiJmZTdjMGNiMiJ9.-bvlFsPKLZPUMwBof2cfxWOSgEVDEgNknNpE3LJLlZc";

https.get(url, (res) => {
    let data = '';
    res.on('data', (chunk) => {
        data += chunk;
    });
    res.on('end', () => {
        try {
            const style = JSON.parse(data);
            
            // Update sources
            if (style.sources && style.sources.carto) {
                const source = style.sources.carto;
                if (source.url && source.url.includes('basemaps.cartocdn.com') && !source.url.includes('auth_token')) {
                    source.url += (source.url.includes('?') ? '&' : '?') + 'auth_token=' + token;
                }
            }

            // Create 3D building layer
            const buildingLayer = {
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
            };

            // Insert before first symbol layer
            let insertIdx = style.layers.length;
            for (let i = 0; i < style.layers.length; i++) {
                if (style.layers[i].type === 'symbol') {
                    insertIdx = i;
                    break;
                }
            }

            style.layers.splice(insertIdx, 0, buildingLayer);

            fs.writeFileSync('app/src/main/assets/style_3d.json', JSON.stringify(style, null, 2));
            console.log("Created style_3d.json successfully");

        } catch (e) {
            console.error("Error parsing JSON:", e);
        }
    });
}).on('error', (err) => {
    console.error("Error fetching URL:", err);
});
