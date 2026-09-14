# Johar Ranchi offline map pack

`ranchi.pmtiles` is generated locally and intentionally not committed through the GitHub text connector.

From the repository root:

```bash
bash tools/maps/build-ranchi-map-pack.sh
```

The build script:

1. downloads the Jharkhand district GeoJSON source;
2. extracts only Ranchi district (`dt_code=364`);
3. clips the Protomaps/OpenStreetMap PMTiles archive to that Ranchi boundary;
4. writes `app/src/main/assets/maps/ranchi.pmtiles`.

At runtime `RanchiMapPackStore` copies the asset into app-private storage because MapLibre PMTiles requires random-access `file://` reads.

The shipped UI must retain OpenStreetMap attribution.
