# Android App w/Image Enhancement Methods w/Python
 Six different methods for Image Enhancement were coded in Python and connected to an Android App through an IP
 
1. Both the cell phone in which the application will be used and the device where the Python script will be run must be connected to the same network. It should be verified that the IP in the last line of the Python script and in lines 214 of the MainActivity.java and 15 of Android's RetrofitClient.java is the same and corresponds to the user's local IP.
2. If the image is taken with the camera, it should be horizontal for better results.
3. The image should ALWAYS be selected from the gallery even if it was taken with the camera. So it is essential to click on the ImageView (the part where the image is displayed, where the icon with the camera is), to be able to access the gallery and upload the image of our interest.
4. Once the image has been selected from the gallery, you must enter the number corresponding to the desired method, and finally press the "START" button.
5. The Sobel method may not run due to timeout with the server, but this depends on the computer.
6. If you click on start and there is nothing in the editText, an error message will be sent indicating that a method must be selected.
