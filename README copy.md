This repo contains a URF system for Android phones to explore the private DCIM collection.

1. Clone repo. 

2. Download files provided in thesis file for the RexNeXt101 model.

3. Set up the image analysis
3.1. Insert your local IP address into ServerClient.java line 36.
3.2. Insert your local IP address into flask_server.py line 107.
3.3. Insert the required files into line 41, 45 and 92 (follow comments in code).
3.4. Install torch, torchvision, Pillow and numpy
3.5. Start the server (before running the app!) with python3 flask_server.py

4. Connect phone and computer to same wifi

5. Install the app
