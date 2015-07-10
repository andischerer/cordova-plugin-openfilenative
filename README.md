# OpenFileNative

fork from https://github.com/markeeftb/FileOpener

@author Andreas Scherer

Simple Plugin which allows yout to open various local and remote files (jpg, png, pdf, ...) in
native Device App. The file mime-type must be supported and you have to install a native App (e.g. Adobe Reader to view PDF Files) to open it.

Supports: Android 2.3+

The license is MIT, so feel free to use, enhance, etc. If you do make changes that would
benefit the community, it would be great if you would contribute them back to the original
plugin, but that is not required.

## Usage
```````javascript
$('body').on('click', 'a.openFileNative', function(e) {
    e.preventDefault();
    window.openFileNative.open($(this).attr('href'));
    /*
    targets for link-href could be:
    - remote file (http://www.foo.bar/sample.jpg)
    - local file (file:///mnt/sdcard/sample.jpg)
    - local file relative (img/sample.jpg) => file in project-assets-www folder
    */
});
```````

## Supported protocols in linktarget
- http://
- https://
- ftp://
- file://
- market://

## License

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be included in all copies
or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

