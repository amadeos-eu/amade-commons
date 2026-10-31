# 28. Webcam

Every laptop has a camera, and most people use it only in video calls. A camera program for the desktop has a smaller job than a video call: show the picture, take a photo, record a video, and leave the camera alone again. Underneath, the job is harder than it looks. The picture comes from a device that can be busy, unplugged or slow. Every camera offers different sizes and controls. And thirty pictures a second have to reach the screen, the encoder and the QR code reader without one of them holding up the others.

amade-webcam has about 4,900 lines of Java and 2,300 lines of tests. It does not talk to the camera itself: one ffmpeg process reads the camera and hands over raw pictures, a second one encodes the video, and `v4l2-ctl` reads and sets the controls. Nothing moves into amade-commons with it. It is the fourth program after Imagescan, Audio and TV that lives mostly from `CommandRunner` and the programs behind it.

## What you build

![amade-webcam with the camera picture, the shutter button and the last three photos and videos](images/webcam/main.png)

- **The live picture** of any Video4Linux camera, built in or on USB. It is mirrored like a mirror, and cameras that are plugged in or pulled out show up within two seconds.
- **Photos** with flash and shutter click, a self-timer of 3 or 10 seconds, and bursts of 3 or 5 photos.
- **Videos with sound** and a level meter. They are saved as MP4 (H.264 and AAC) by default, or WebM (VP9 and Opus).
- **Time lapse**: one picture every 1, 5, 10 or 30 seconds, played back at the normal frame rate.
- **Looks**: black and white, sepia, warm and cool, in the picture and in what is saved.
- **QR codes**, with a fitting action for a link, an e-mail address, a WLAN password or text.
- **The newest three photos and videos** next to the shutter, with Open, Edit, Show in Folder and Delete.
- **Camera settings**: camera, size, frame rate, microphone, formats, folders, and the picture controls that this camera offers, kept per camera.
- **Full screen** and five languages.

The screenshots show a drawn room instead of a camera picture: `--test-picture=scene.png` makes a still image the camera ([see below](#a-camera-for-tests)).

## What you will learn

- How to read a camera with ffmpeg and turn its output into `BufferedImage`s without copying a byte.
- How to find cameras in `/sys`, and ask a camera for its sizes and controls with `v4l2-ctl`.
- How to hand thirty frames a second from a reader thread to the screen without a queue that grows.
- How to record from the same frames with a second ffmpeg, with sound from PulseAudio and a level meter from ffmpeg's own filter.
- How to tell the user why there is no picture: busy, gone, not supported, program missing.
- How to read QR codes with ZXing a few times a second without slowing the picture down.
- How to make a program with a camera testable without a camera.

## The structure

| Package | Contents |
|---|---|
| `domain` | `CameraDevice`, `VideoMode`, `CameraControl`, `Look`, `CaptureMode`, `PhotoTimer`, `Shots`, `VideoSpeed`, `PhotoFormat`, `VideoFormat`, `FileNames`, `QrContent` |
| `repository` | `WebcamPreferences` (`webcam.properties`: last camera, size and controls per camera, folders, formats, options) |
| `service` | `CameraFinder` and `V4l2Output` (devices, sizes, controls), `FrameSource` and `FfmpegCamera`, `Recorder`, `Microphones`, `QrReader`, `PhotoSaver`, `RecentMedia`, `ShutterSound`, `TestPictureCameras` |
| `ui.main` | `MainWindow`, `MainPresenter`, `MainMenuBar`, `PreviewPanel`, `ShutterButton`, `LevelMeter`, `RecentStrip`, `QrBanner` |
| `ui.settings` | `SettingsDialog` |

The presenter gets everything that touches a device, a program or the clock in one record, `Services`, put together in the module:

```java title="amade-webcam · bootstrap/WebcamModule.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/bootstrap/WebcamModule.java:presenter"
```

`Ticker` is a small interface for "run this every n seconds", so the tests use a ticker they advance by hand. With the clock as a `Supplier<LocalDateTime>`, a test knows the file name of a photo before it is taken.

## Finding cameras

Linux lists every Video4Linux device in `/sys/class/video4linux`, one directory per node (`video0`, `video1`, ...) with a file `name`. A USB camera has its vendor and product number two directories up, in `idVendor` and `idProduct`. That is enough to tell two cameras of the same model apart from a third one, and to remember the settings of each.

Not every node is a camera. Most USB cameras register two nodes: one with pictures and one with metadata. So `CameraFinder` asks each node for its formats with `v4l2-ctl --list-formats-ext`, and keeps only the nodes that have some:

```java title="amade-webcam · service/CameraFinder.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/service/CameraFinder.java:cameras"
```

The formats are cached per node, because `v4l2-ctl` takes a moment and the list is needed again and again. A camera plugged in later can get the node of one that was pulled out, so the cache forgets every node that is gone.

`V4l2Output` parses what `v4l2-ctl` prints: formats with sizes and frame rates, and controls with their type, range, default, current value and, for menus, their entries. It is a class of static functions on strings, tested with output copied from real cameras. Nothing in it starts a program.

### Plugged in, pulled out

There is no simple event for a new camera without udev bindings, so the presenter looks every two seconds. Looking costs almost nothing as long as it only reads the directory names and small files. The expensive part, asking each node for its formats, runs only when the list of nodes has changed:

```java title="amade-webcam · ui/main/MainPresenter.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/ui/main/MainPresenter.java:plug"
```

Three threads take part, and each does what it is good at. The ticker thread reads `/sys`. The event dispatch thread compares the lists, because it owns them. The background executor runs `v4l2-ctl`.

## Reading the camera

Java has no camera API. There are libraries that bind to Video4Linux through JNI, but ffmpeg already speaks to every camera, decodes MJPEG and YUYV, and is installed on every amadeOS system. So `FfmpegCamera` starts ffmpeg and asks it for raw pictures on standard output:

```java title="amade-webcam · service/FfmpegCamera.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/service/FfmpegCamera.java:class"
```

`bgr24` is the choice that matters. Java's `BufferedImage.TYPE_3BYTE_BGR` keeps its pixels in exactly that order: blue, green, red, three bytes per pixel, row by row. So the reader creates the image first and lets ffmpeg's bytes go straight into its buffer:

```java title="amade-webcam · service/FfmpegCamera.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/service/FfmpegCamera.java:reader"
```

`readNBytes` waits until a whole picture is there. A new image for every frame looks wasteful. But each frame is passed on to the screen, maybe to the encoder and the QR reader, and none of them must see it change. One allocation of 2.7 MB thirty times a second is no problem for the garbage collector, and it is much simpler than a pool of buffers with reference counts.

The `scale` filter in the command keeps the size fixed. A camera may deliver another size than it was asked for, and a reader that expects 1280 × 720 must never get anything else.

### Why there is no picture

When ffmpeg ends, its last lines on standard error say why. `FfmpegCamera` keeps the last few kilobytes and turns them into one of five reasons:

```java title="amade-webcam · service/FfmpegCamera.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/service/FfmpegCamera.java:classify"
```

The window shows a camera sign with a sentence for each. *The camera is in use by another program* is the most common one, and it needs a different answer than *The camera was disconnected*. The fifth reason, `PROGRAM_MISSING`, comes from `CommandRunner`'s `ProgramMissingException` when ffmpeg is not installed ([chapter 31](31-imagescan.md)).

## From the reader to the screen

The reader thread delivers frames as fast as the camera does. The event dispatch thread may be busy with a menu, or the window may be hidden. If every frame became a task on the EDT, the tasks would pile up and the picture would run behind. So only one frame ever waits:

```java title="amade-webcam · ui/main/MainPresenter.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/ui/main/MainPresenter.java:frame"
```

`pendingFrame` is an `AtomicReference`. The first frame finds it empty and schedules one task. Later frames only replace what waits, and the task shows the newest one when it runs. On a slow machine the picture drops frames, but it never runs behind.

The look is applied right here, on the reader thread, before anyone else sees the frame. So the screen, the photo and the video all have it. It works on the bytes directly, which is fast enough for 30 frames a second without any library:

```java title="amade-webcam · domain/Look.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/domain/Look.java:apply"
```

The mirror is not in the bytes. `PreviewPanel` draws the picture with its x-coordinates swapped, which costs nothing. Photos and videos are saved the right way round unless the user asks for mirrored files; then `PhotoSaver.mirror` makes a copy.

## Photos

A photo is the newest frame, written as JPEG (quality 0.92) or PNG. The white flash and the shutter click are what tell the user that it happened. The flash is an overlay in `PreviewPanel` that fades out in a fifth of a second. The click is synthesized as a short noise burst with a falling envelope and played through Java Sound, so the program ships no sound file.

The self-timer and bursts are timing, and timing belongs in the presenter, which gets its time from the `Ticker`. A countdown is a ticker task every second that shows the number and takes the photo at zero. A burst takes the next photo every 250 ms. Because the tests advance a manual ticker, they check a 10-second timer and a burst of five in microseconds.

`FileNames` gives *Photo 2026-10-31 14-05-12.jpg*. A burst or a second photo in the same second gets *(2)*, *(3)*. The file is written to a `.part` name and moved into place, as in Image Editor ([chapter 39](39-imageeditor.md#saving)), so the recent strip and the file manager never see half a photo.

## Videos

The preview must keep running while a video is recorded. So the recorder does not open the camera again: a second ffmpeg gets the same frames on its standard input.

```java title="amade-webcam · service/Recorder.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/service/Recorder.java:command"
```

A few options carry the design:

- **`-use_wallclock_as_timestamps 1`** stamps each frame with the time it arrives. Frames that the camera drops in poor light then make a gap in time, not a video that runs too fast. `-fps_mode cfr -r 30` fills such gaps with copies, so players get a constant frame rate.
- **`-f pulse`** reads the microphone through PulseAudio, which on amadeOS is PipeWire. `Microphones` lists the sources with `pactl -f json list sources` and leaves out the `.monitor` sources, which record what the computer plays.
- **The level meter** comes from ffmpeg too. The `astats` filter measures the peak of each tenth of a second, and `ametadata` prints it to standard output, which the recorder reads in a thread of its own. The meter shows what is really recorded, not what another program hears.
- **`-shortest`** ends the sound with the picture. Closing standard input is how the recording stops, and the sound must stop then too.
- **MP4 or WebM** is a list of options from `VideoFormat`. MP4 uses `libx264` with `-movflags +faststart`, so the index is at the start and the video plays while it downloads. WebM uses `libvpx-vp9` and `libopus`. MP4 is the default because it plays on every phone, TV and computer, WebM is there for those who want an open format all the way.

The frames go to the encoder through a queue of eight frames, which a writer thread empties:

```java title="amade-webcam · service/Recorder.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/service/Recorder.java:write"
```

The camera thread offers each frame to the queue. For a normal video the offer does not wait: if the encoder falls behind, a frame is dropped and the timestamps keep the video in time. A time lapse keeps one frame every few seconds, and each of them counts, so there the offer waits up to two seconds for room:

```java title="amade-webcam · service/Recorder.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/service/Recorder.java:offer"
```

A time lapse uses `-framerate` instead of wall-clock timestamps, so the kept frames follow each other at the normal frame rate. That is what makes an hour of clouds into a few seconds.

!!! warning "The first seconds wait in memory"
    With sound, ffmpeg holds the first seconds back until picture and sound are lined up, and the file stays at a few bytes for up to 15 seconds. The status bar therefore says that ffmpeg is still collecting the first seconds instead of showing *0 KB*.

When the camera goes away during a recording, the presenter stops the recorder before it shows the message. ffmpeg then finishes the file, and what was recorded is kept.

## The camera's controls

Each camera offers its own controls, and `v4l2-ctl --list-ctrls-menus` lists them with type, range, default and current value. `CameraControl.SHOWN` is the list of controls the settings dialog shows, in a fixed order: brightness, contrast, saturation, sharpness, white balance, exposure, power line frequency and a few more. A camera without one of them simply doesn't get a row. Integers become sliders, booleans checkboxes, menus combo boxes. Controls that the camera marks *inactive*, such as the colour temperature while automatic white balance is on, are greyed out and enabled again when the automatic setting is switched off.

![The camera settings: on the left camera, size, microphone, formats and folders, on the right the controls this camera offers](images/webcam/settings.png)

A slider sends many values a second while it is dragged, and each value would start `v4l2-ctl`. The dialog waits until the slider has rested for 120 ms and sends only the last value. The presenter stores the values per camera, using the USB id as the key, and sets them again when that camera is opened.

## QR codes

[ZXing](https://github.com/zxing/zxing) is the standard Java library for barcodes. Only its core is needed. `QrReader` turns the frame into a grey picture of at most 800 pixels width and hands it to ZXing as luminance:

```java title="amade-webcam · service/QrReader.java"
--8<-- "amade-webcam/src/main/java/eu/amadeos/apps/webcam/service/QrReader.java:read"
```

The search takes some tens of milliseconds, so it runs on the background executor, at most every 300 ms, and only when the previous search is done. An `AtomicBoolean` keeps a second search from starting. `QrContent` decides what the text is: a link, `mailto:`, `tel:`, a WLAN in the `WIFI:T:WPA;S:name;P:password;;` format with its escaped `;`, `:` and `\`, or plain text. The banner offers the fitting button. A banner the user closed stays closed until the code has been out of the picture for four seconds, so it doesn't pop up again at once.

## A camera for tests

A program for a device needs a way to run without the device: for the tests, for the screenshots in this book, and for anyone who works on the code on a computer without a camera. `--test-picture` replaces the camera with ffmpeg's moving test pattern `testsrc2`, and `--test-picture=FILE` with a still image that ffmpeg loops at the frame rate. In both cases the same reader, presenter and recorder run as with a real camera. `TestPictureCameras` gives a fake camera with a few sizes and controls, so even the settings dialog has something to show.

The tests work on three levels:

- **Parsers and domain**: `V4l2Output` with copied output of real cameras, `QrContent`, `FileNames`, `Look` on single pixels.
- **Services with the real ffmpeg**: `FfmpegCameraTest` reads real frames from `testsrc`. `RecorderTest` records a real MP4 with sound and a real WebM time lapse, and checks them with `ffprobe`: codecs, size, a duration that fits. The CI workflow installs ffmpeg for it.
- **The presenter with fakes**: a fake `FrameSource` that delivers frames on command, a fake recorder, a manual ticker and a fixed clock. The tests drive the timer, bursts, recording, cameras that come and go, the QR banner and exit during a recording through a fake `MainView`, without Swing.

The build fails if any test is skipped. On a machine without a display the window tests would skip themselves, so the workflow runs them under Xvfb.

## What amade-commons gives Webcam

| Part | Since | Webcam uses it for |
|---|---|---|
| `AmadeApplication`, `AppInfo`, `CommonsModule`, `EdtExecutor` | chapter 10 | start-up and wiring, `--version` |
| `Messages`, `Mnemonics`, `LocaleSettings`, `TranslationCheck` | chapter 10 | five languages, menu letters, the language menu, the translation test |
| `AmadeUI`, `DesktopTheme` | chapter 10 | the Motif look and the theme colours |
| `DesktopLauncher` | chapter 11 | opening photos and videos, *Show in Folder*, links from QR codes |
| `AppDirectories`, `PropertiesFile`, `XdgDirectories` | chapter 20 | `webcam.properties`, the Pictures and Videos folders |
| `@Ui` | chapter 30 | the EDT executor in the presenter |
| `CommandRunner`, `ProgramMissingException` | chapter 31 | ffmpeg, `v4l2-ctl` and `pactl`, and the message when one is missing |
| `Json` | chapter 33 | the microphone list from `pactl -f json` |

### And what stays in Webcam

- **The camera.** `CameraFinder`, `V4l2Output` and `FfmpegCamera` are the first code that talks to a camera. A video call program or a scanner front end for document cameras would be the second user.
- **The recorder.** TV records with ffmpeg too ([chapter 38](38-tv.md)), but it copies a stream into a file, while Webcam encodes frames it gets on standard input. The two share only "start ffmpeg, stop it with `q` or by closing its input, wait", which is `CommandRunner` already.
- **The recent strip.** Thumbnails of the newest photos and videos in a folder, with a frame from ffmpeg for each video, exist only here. If Screenshot or Image Scan get a strip of their last pictures, it moves to amade-commons.

## Exercises

1. **udev instead of polling.** Replace the two-second look at `/sys` with a thread that reads udev events from a netlink socket, through JNA. What does the presenter have to change, and how do you keep the tests without a kernel?
2. **Background blur.** Blur everything but the person. Start with a cheap version: a fixed oval in the middle stays sharp, the rest is blurred on a scaled-down picture. Why does the blur have to run before the frame reaches the recorder, and how much time per frame do you have at 30 fps?
3. **Pause a recording.** Add *Pause*. Stopping the frames is easy. What happens to the wall-clock timestamps, and to the sound from PulseAudio?
4. **A second microphone level.** Show the level meter before the recording starts, so the user can check the microphone. Which program reads the microphone then, and how do you avoid two programs fighting over it during the recording?
5. **Barcodes on products.** Let the QR reader also read EAN-13 codes and offer to search the number. Which ZXing hints change, and why does the reader need the full picture width for them?

## Summary

- Let ffmpeg read the camera and write `bgr24` to a pipe: a `TYPE_3BYTE_BGR` image takes those bytes as they come.
- Find cameras in `/sys/class/video4linux`, keep only the nodes with formats, and recognise a camera by its USB id.
- Look for new cameras cheaply and often, and do the expensive search only when something changed.
- Hand frames to the screen through one waiting slot, not a queue. The picture may drop frames, but it never runs behind.
- Record from the same frames with a second ffmpeg: wall-clock timestamps for normal videos, a fixed rate for a time lapse, sound and the level meter from PulseAudio through ffmpeg.
- Tell the user why there is no picture, in words that fit: busy, gone, not supported, program missing.
- Send a slider's value only when it rests, and keep the controls per camera.
- Give a device program a test picture, so it can be tested, shown and developed without the device.
