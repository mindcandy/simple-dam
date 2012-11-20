//
// script to automate creating 96x96 thumbnail jpeg and 320x320 preview jpeg from anything Photoshop can load
// happily this includes all image files and AI/PDF files as well!
//
// TO USE: Open Photoshop CS3 (or later) and go to File -> Scripts -> Browse.. and then select this file
//         You will be prompted for a folder to scan for image files. 
//         Previews/Thumbnails will be generated next to original files.
//         Any errors will be reported per-file and processing will continue.
//

// enable double clicking from the Macintosh Finder or the Windows Explorer
#target photoshop

app.bringToFront();

main();

function main() {
    try {
        var sourceFolder = Folder.selectDialog(
            "Select Folder to create thumbnails and previews." +
            "\nSupported file types: ai, pdf, png, gif, jpg, bmp, tif, psd");

        if (sourceFolder) {
            
            // save old preferences
            var oldPref = app.preferences.rulerUnits;   
            app.preferences.rulerUnits = Units.PIXELS; 

            var files = findFiles(sourceFolder);
            for (var i = 0; i < files.length; i++) {
                // for each folder 
                ProcessFile(files[i]);
            }

             // restore old prefs    
            app.preferences.rulerUnits = oldPref;
        }
        files = null;
        sourceFolder = null;
        oldPref = null;
    }
    catch( e ) {
        // it go boom
        // Give a generic alert and see if they want the details
        if ( DialogModes.NO != app.playbackDisplayDialogs ) {
          alert( e + " : " + e.line );
        }
    }
}

function ProcessFile(file) {
    try {
        // load file
        app.open(file);

        // Trim empty space
        TrimImage();

        // resize to Preview
        ResizeImage(320,320);

        // save image jpeg
        SaveJPEG(file, "_preview.jpg", 80, true);

        // resize again
        ResizeImage(96,96);

        // save image thumbnail jpeg
        SaveJPEG(file, "_thumbnail.jpg", 40, false);

        // close the file
        app.activeDocument.close(SaveOptions.DONOTSAVECHANGES)
    }
    catch (e) {
        if ( DialogModes.NO != app.playbackDisplayDialogs ) {
          alert("Error processing a file\nFile: " + file.fsName + "\nError: " + e + " : " + e.line );
        } 
    }
}


function hasBackgroundLayer(document) {
    var layers = document.layers;
    for (var i = 0; i < layers.length; i++) {

        if (layers[i].isBackgroundLayer) {
            return true;
        }
    }
    return false;
}

function TrimImage() {
    if (hasBackgroundLayer(app.activeDocument)) {
        // can't use transparency to trim!
        app.activeDocument.trim(TrimType.TOPLEFT);    
    } else {
        // can trim transparent images
        app.activeDocument.trim(TrimType.TRANSPARENT);
    }
}

function SaveJPEG(originalFile, suffix, quality, includeProfile) {
    
    var doc = app.activeDocument;
    var docName = originalFile.displayName
    var parentFolder = originalFile.parent.fsName
    var lastDot = docName.lastIndexOf( "." );
    if ( lastDot == -1 ) {
        lastDot = docName.length;
    }
    var fileNameNoPath = docName.substr( 0, lastDot );
    
    var outputFile = new File(parentFolder + "/" + fileNameNoPath + suffix);
    // alert("going to write to file\n " + outputFile.fsName);

    var tempFile = new File(parentFolder + "/" +  "__temp_" + suffix);

    var white = new RGBColor();
    var options = new ExportOptionsSaveForWeb();

    options.format = SaveDocumentType.JPEG;
    options.optimized = true;
    options.quality = quality;
    options.includeProfile = includeProfile;
    options.matteColor = white;

    // export to temp file and copy over -- deals with overwriting existing files and SaveForWeb bugs
    app.activeDocument.exportDocument(tempFile, ExportType.SAVEFORWEB, options);
    tempFile.copy(outputFile);
    tempFile.remove();
}

// resize an image - w/h in pixels
// this is entirely 
function ResizeImage(width, height) {

    var docWidth;
    var docHeight;
    var docRatio;
    var newWidth;
    var newHeight;
    var resolution = app.activeDocument.resolution;
    

    // original width, height
    docWidth = (1.0 * app.activeDocument.width * resolution) / 72.0; // decimal inches assuming 72 dpi (used in docRatio)
    docHeight = (1.0 * app.activeDocument.height * resolution) / 72.0; // ditto
    
    if (docWidth < 1.0 || docHeight < 1.0)
        return true; // error
    
    if (width < 1 || height < 1)
        return true; // error
    
    docRatio = docWidth / docHeight; // decimal ratio of original width/height
    
    newWidth = width;
    newHeight = ((1.0 * width) / docRatio) + 0.5; // decimal calc
    newHeight = 1 * newHeight; // make integer
    
    if (newHeight > height) {
        newWidth = 0.5 + docRatio * height; // decimal calc
        newWidth = 1 * newWidth; // make integer
        newHeight = height;
    }
    
    // resize the image using a good conversion method while keeping the pixel resolution
    // and the aspect ratio the same
    app.activeDocument.resizeImage(newWidth, newHeight, resolution, ResampleMethod.BICUBICSHARPER);
    return false; // no error
}


///////////////////////////////////////////////////////////////////////////////
// findFiles - get all files within the specified source
///////////////////////////////////////////////////////////////////////////////
function findFiles(sourceFolder) {

    // declare local variables
    var fileArray = new Array();
    var extRE = /\.(?:ai|pdf|png|gif|jpg|jpeg|bmp|tif|tiff|psd)$/i;
    var excludedRE = /(_preview|_thumbnail)\.\w+$/;

    // get all files in source folder
    var docs = Folder( sourceFolder ).getFiles();
    var len = docs.length;
    for (var i = 0; i < len; i++) {
        var doc = docs[i];

        // only match files (not folders)
        if (doc instanceof File) {
            // store all recognized files into an array
            var docName = doc.name;
            if (docName.match(extRE) && !docName.match(excludedRE)) {
                fileArray.push(doc);
            }
        }
    }

    // return file array
    return fileArray;
}

