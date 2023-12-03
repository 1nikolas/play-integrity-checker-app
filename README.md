<img src="https://github.com/1nikolas/play-integrity-checker-app/raw/main/app/src/main/ic_launcher-playstore.png" width="96" align="right" />

# Play Integrity API Checker

Get info about your Device Integrity through the Play Integrity API
     
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=gr.nikolasspyr.integritycheck)

## Important Note
If you want to implement the Play Integrity API in your app you shouldn't do it this way. The API server should not send the whole JSON to the app, only a yes/no. Also ideally you should pair the integrity request with another one (for example login). That way your API won't let the user proceed without a valid Integrity token that passes integrity checks (even if your app is reverse engineered).

## Setup
In order to run this yourself you'll need:
1) The [Play Integrity Checker Server](https://github.com/1nikolas/play-integrity-checker-server)
2) Your server url specified in `local.properties` like this:
```
API_URL=https://my-awesome-server-url.com
```
3) The app to be on Play Store (otherwise you won't have access to `MEETS_BASIC_INTEGRITY` and `MEETS_STRONG_INTEGRITY`)
4) Play Integrity linked to a Google Cloud project through the Play Console with `MEETS_BASIC_INTEGRITY` and `MEETS_STRONG_INTEGRITY` enabled

![](https://user-images.githubusercontent.com/30593419/180609045-fe0da305-24d9-4ffb-a44d-4294a759c787.png)

To set up your Google Cloud project see [here](https://github.com/1nikolas/play-integrity-checker-server#how-to-set-up-google-cloud)

## License

MIT License

```
Copyright (c) 2022 Nikolas Spiridakis

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
