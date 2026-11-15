# Third-party components

## Legacy Android Auto SDK binary

- File: `tube/libs/aauto.aar`
- Retrieved from: https://raw.githubusercontent.com/slashmax/AAMirror/master/mirror/libs/aauto.aar
- Repository blob SHA-1: `678f4afb0434ceb97b23c2237ce4489bc52b4394`
- Downloaded file SHA-256: `b7509a5a54672a9bd4e822c8aec8f722c26e71a4a6710c502b53b26c41c81c04`
- Contains `com.google.android.apps.auto.sdk` and legacy `com.google.android.gms.car` interfaces.
- Public distribution/license permission for this historical SDK binary was not established. This is an unresolved dependency for any public/commercial release; do not represent the SDK as user-owned or as officially supported by Google.
- AAMirror's source was consulted to identify the legacy service declarations. No AAMirror application source was copied into this project.

## Android support libraries

`com.android.support:support-v4:25.3.1` and `com.android.support:recyclerview-v7:25.3.1`, resolved from Google's Maven repository. These legacy versions satisfy the historical Auto SDK's `android.support` interfaces. Their upstream Apache 2.0 notices and dependency metadata remain in the libraries/build metadata.


AndroidX WebKit 1.12.1: Apache 2.0. Fermata and TizenTube were studied as architecture references; their application source was not copied. See tube/README.md.
