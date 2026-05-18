# Centroid Finder Video Part II Plan:

## We will be using JavaCV as our video library.

1. We are going to treat the video as an image for each frame, possible maybe every 24 frames to show a second, whatever long the video is (maybe on the front end you can choose how many frames to check for the position of the salamander).
2. Using each image frame, we will use the built in ImageSummaryApp that was created.
3. It will binarize its image by the selected color, and output some data of the biggest size, x and y coordinate.
4. There will definitely be a while loop that will continuously output the data that adds to the excel file of size, x and y coordinates.
5.
