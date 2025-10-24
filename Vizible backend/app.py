from fastapi import FastAPI
from ultralytics import YOLO
import cv2
import threading
import time

app = FastAPI()

# Load the YOLOv8 model pretrained on COCO dataset
model = YOLO('yolov8s.pt')  # Ensure yolov8s.pt is downloaded and in the same directory

# Camera server links (replace with your actual RTSP/HTTP stream URLs)
front_camera_url = 'rtsp://your_front_camera_ip/stream'  # e.g., 'rtsp://192.168.1.100:554/stream'
left_camera_url = 'rtsp://your_left_camera_ip/stream'   # e.g., 'rtsp://192.168.1.101:554/stream'
right_camera_url = 'rtsp://your_right_camera_ip/stream' # e.g., 'rtsp://192.168.1.102:554/stream'

# Global variables to store latest detections
detections = {
    'Front': [],
    'Left': [],
    'Right': []
}

def detect_objects(camera_url, direction):
    cap = cv2.VideoCapture(camera_url)
    if not cap.isOpened():
        print(f"Error: Could not open camera stream for {direction} at {camera_url}")
        return
    
    while True:
        ret, frame = cap.read()
        if not ret:
            print(f"Failed to read frame from {direction}")
            time.sleep(1)
            continue
        
        # Run YOLO detection
        results = model(frame)
        
        # Extract detected class names (COCO classes)
        detected_classes = []
        for result in results:
            for box in result.boxes:
                class_id = int(box.cls)
                class_name = model.names[class_id]
                if class_name not in detected_classes:
                    detected_classes.append(class_name)
        
        # Update global detections
        detections[direction] = detected_classes
        
        time.sleep(1)  # Adjust detection frequency as needed (e.g., every second)
    
    cap.release()

# Start detection threads for each direction
threading.Thread(target=detect_objects, args=(front_camera_url, 'Front')).start()
threading.Thread(target=detect_objects, args=(left_camera_url, 'Left')).start()
threading.Thread(target=detect_objects, args=(right_camera_url, 'Right')).start()

@app.get("/detections")
async def get_detections():
    # Format the response as specified
    front_str = ','.join(detections['Front'])
    left_str = ','.join(detections['Left'])
    right_str = ','.join(detections['Right'])
    
    response = f"Front:{{{front_str}}} | Left:{{{left_str}}} | Right:{{{right_str}}}"
    return {"detections": response}

if __name__ == '__main__':
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)