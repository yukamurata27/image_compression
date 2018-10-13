# Image Compression

## Introduction
This program compresses an image whose size is standard CIF size 352x288. The sample image files can be found in this directory. The vector compression technique is used and take 2 pixels side by side.

## How to compile and run the program
```java
javac ImageCompression.java
java ImageCompression FILE_NAME NUM_OF_VECTOR
```

Example:
```java
javac ImageCompression.java
java ImageCompression image1.raw 16
```
