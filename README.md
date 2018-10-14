# Image Compression

## Introduction
This program compresses an image whose size is standard CIF size 352x288. The sample image files can be found in this directory. The vector compression technique is used and take 2 pixels side by side. The compressed image containes N colors that best represent the original image.

## How to compile and run the program
```
javac ImageCompression.java
java ImageCompression FILE_NAME NUM_OF_VECTOR
```

Example:
```
javac ImageCompression.java
java ImageCompression image1.raw 16
```
