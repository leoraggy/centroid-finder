## Image Summary App Notes

1.  Takes in an image, hex value for the target color, and threshold for how white it wants to be.
2.  After it gets processed, the hex value will be parsed into a 24 bit integer.
3.  Binarize the image
4.  Write the binarized image to disk as "binarized.png"
5.  Create an ImageGroupFinder using a BinarizingImageGroupFinder with a DFS-based BinaryGroupFinder

PUSHING VIDEO BRANCH
