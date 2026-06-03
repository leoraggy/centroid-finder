1. Refactoring code (required)
   Currently theres a bug that saves every frame of the image, I should remove the code. And perhaps only have 1.
   Refactor API to allow proxy/CORS issue.
2. Adding tests (required)
   Unit tests for pure logic: Add tests for ColorDistanceFinder, ImageBinarizer, DfsBinaryGroupFinder, and Group behavior (you already have some tests; expand coverage).
   Test for more nulls or throwing.
3. Improving error handling (required)
   In video summary app there should be a range between the threshold, for example check if its greater than 0 and less than 100?
   Improve exception messages: Provide actionable errors (include parameter values and expected formats) and log exceptions with context (e.g., which file/frame failed).
4. Writing documentation (required)
   CLI README: Add a short README for the processor module with usage examples, expected CSV schema, and recommended runtime memory/CPU.
   Put a description on the video summary app for those who don't know how to use it.
