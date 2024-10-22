# Thumbnail Rebuilder

[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-blue.svg?logo=kotlin)](httpw://kotlinlang.org)
![WASM](https://img.shields.io/badge/-WASM-gray.svg?style=flat)

A web-based utility designed for the reconstruction of embedded thumbnails within JPEG files.

## Technologies used

* [Kotlin](https://kotlinlang.org/) for WebAssembly (Kotlin/WASM)
* [skiko wasmJS](https://github.com/JetBrains/skiko) for thumbnail generation
* [Ashampoo Kim](https://github.com/ashampoo/kim) for embedding the thumbnail into EXIF metadata

## Primary use cases for thumbnail reconstruction

* Enhancing the performance of applications such as  [Ashampoo Photos](https://ashampoo.com/photos) and Apple Finder that make use of embedded thumbnails.
* Rectifying issues with broken or low-quality thumbnails, particularly when Apple Finder or Windows Explorer display incorrect thumbnail representations.

## Contributions

Contributions to this project are welcome! If you encounter any issues,
have suggestions for improvements, or would like to contribute new features,
please feel free to submit a pull request.

## License

Thumbnail Rebuilder is licensed under the GNU Affero General Public License (AGPL),
ensuring the community's freedom to use, modify, and distribute the software.
