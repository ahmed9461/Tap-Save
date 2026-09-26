`fixture.mp4` is generated test media: 0.5 seconds of black 64x64 H.264 video and silent AAC mono audio (2,553 bytes). It contains no third-party or user media and is packaged only in the test APK.

Recreate using FFmpeg:

```sh
ffmpeg -f lavfi -i color=c=black:s=64x64:r=10 -f lavfi -i anullsrc=r=44100:cl=mono -t 0.5 -c:v libx264 -pix_fmt yuv420p -c:a aac -movflags +faststart fixture.mp4
```
