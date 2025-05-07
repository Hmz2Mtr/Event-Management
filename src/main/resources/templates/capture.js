let userName = '';
let hasCaptured = false;

cv['onRuntimeInitialized'] = () => {
    document.getElementById('startCapture').addEventListener('click', () => {
        userName = document.getElementById('name').value;
        if (!userName) {
            document.getElementById('status').innerText = 'Please enter your name.';
            return;
        }
        startCamera();
    });
};

function startCamera() {
    const video = document.getElementById('video');
    navigator.mediaDevices.getUserMedia({ video: true })
        .then(stream => {
            video.srcObject = stream;
            const net = cv.readNetFromDarknet('/static/yolo-face.cfg', '/static/yolo-face.weights');
            setInterval(() => processFrame(net), 100);
        })
        .catch(err => {
            document.getElementById('status').innerText = 'Camera access denied.';
            console.error(err);
        });
}

function processFrame(net) {
    const video = document.getElementById('video');
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    let src = cv.imread(canvas);

    let blob = cv.blobFromImage(src, 1 / 255.0, new cv.Size(416, 416), new cv.Scalar(0, 0, 0), true, false);
    net.setInput(blob);
    let outs = new cv.MatVector();
    net.forward(outs);

    const detections = processYOLOOutputs(outs, src.cols, src.rows);
    const bestBox = getBestBox(detections);

    if (bestBox && bestBox.confidence > 0.8 && isGoodPosition(bestBox) && !hasCaptured) {
        let faceImg = src.roi(bestBox.rect);
        let encoded = new cv.MatVector();
        cv.imencode('.jpg', faceImg, encoded);
        let buffer = encoded.get(0).data;
        let base64 = btoa(String.fromCharCode(...buffer));

        fetch('/store', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ image: base64, name: userName })
        })
            .then(response => {
                if (response.ok) {
                    document.getElementById('status').innerText = 'Picture captured and stored!';
                    hasCaptured = true;
                } else {
                    document.getElementById('status').innerText = 'Error storing picture.';
                }
            })
            .catch(err => {
                document.getElementById('status').innerText = 'Network error.';
                console.error(err);
            });

        faceImg.delete();
        encoded.delete();
    }

    src.delete();
    blob.delete();
    outs.delete();
}

function processYOLOOutputs(outs, imgWidth, imgHeight) {
    const detections = [];
    for (let i = 0; i < outs.size(); i++) {
        const data = outs.get(i).data32F();
        const rows = outs.get(i).rows;
        const cols = outs.get(i).cols;
        for (let j = 0; j < rows; j++) {
            const confidence = data[j * cols + 4];
            if (confidence > 0.5) {
                const x = data[j * cols] * imgWidth;
                const y = data[j * cols + 1] * imgHeight;
                const w = data[j * cols + 2] * imgWidth;
                const h = data[j * cols + 3] * imgHeight;
                const x1 = Math.max(0, x - w / 2);
                const y1 = Math.max(0, y - h / 2);
                detections.push({
                    rect: new cv.Rect(x1, y1, w, h),
                    confidence
                });
            }
        }
    }
    return detections;
}

function getBestBox(detections) {
    if (!detections.length) return null;
    return detections.reduce((best, curr) =>
        !best || curr.confidence > best.confidence ? curr : best, null);
}

function isGoodPosition(box) {
    const centerX = box.rect.x + box.rect.width / 2;
    const centerY = box.rect.y + box.rect.height / 2;
    return centerX > 200 && centerX < 440 &&
        centerY > 100 && centerY < 380 &&
        box.rect.width > 100 && box.rect.height > 100;
}