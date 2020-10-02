import flask
from flask import request,jsonify,json
import werkzeug
import ResNetModel_dk
import PIL
from PIL import Image
import glob
import struct
import os	
from zipfile import ZipFile
import concurrent.futures
from concurrent.futures import ThreadPoolExecutor

app = flask.Flask(__name__)


imagefiles_list = list()
vector_list = list()


ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

@app.route('/', methods=['GET', 'POST'])
def handle_request():  
    # if request.method == 'POST':
    files_id = list(flask.request.files)
    dirpath = os.getcwd()
    print("\nNumber of Received zips : ", len(files_id))
    collection = list()
    for file_id in files_id:
        zippedImageBatch = flask.request.files[file_id]
        filenameString = werkzeug.utils.secure_filename(zippedImageBatch.filename)
        sub_directory = dirpath + filenameString
        print("\nReceived : " + filenameString)
        zippedImageBatch.save(filenameString)
        # Create a ZipFile Object and load sample.zip in it
        with ZipFile(filenameString, 'r') as zipObj:
        # Extract all the contents of zip file into sub_directory
            zipObj.extractall(sub_directory) # should not be same directory
            print("\n extracted")
            print(sub_directory) 
        number_images_in_Folder = len(os.listdir(sub_directory))   

        executor = ThreadPoolExecutor(6)
        futures = [executor.submit(try_multiple_operations, group, sub_directory, collection)
           for group in grouper(os.listdir(sub_directory), 20)]
        concurrent.futures.wait(futures)
            
        print('one batch analysed!')
    return jsonify({'vector_collection': collection})

def try_multiple_operations(imgFolder, sub_directory, collection):
    current = 1
    number_images_in_Folder = len(imgFolder)
    print('images in folder ', number_images_in_Folder)
    ResNetModel_dk.load_n_Label_to_Idx()
    ResNetModel_dk.load_word_Label_List()
    for img in imgFolder:
            try:
                f = Image.open(sub_directory + '/' +img)
                print('opened img file')
                data = ResNetModel_dk.apply_model(f, img)
                print('data ', data)
                collection.append(data)
            except:
                print('no image left in list or error with item')

def grouper(imgList, n, fillvalue=None):
    "Collect data into fixed-length chunks or blocks"
    # grouper('ABCDEFG', 3, 'x') --> ABC DEF Gxx"
    print(iter(imgList))
    args = [iter(imgList)] * n
    return zip_longest(*args, fillvalue=fillvalue)


def zip_longest(*args, fillvalue=None):
    # zip_longest('ABCD', 'xy', fillvalue='-') --> Ax By C- D-
    iterators = [iter(it) for it in args]
    num_active = len(iterators)
    if not num_active:
        return
    while True:
        values = []
        for i, it in enumerate(iterators):
            try:
                value = next(it)
            except StopIteration:
                num_active -= 1
                if not num_active:
                    return
                iterators[i] = repeat(fillvalue)
                value = fillvalue
            values.append(value)
        yield tuple(values)


def repeat(object, times=None):
    # repeat(10, 3) --> 10 10 10
    if times is None:
        while True:
            yield object
    else:
        for i in range(times):
            yield object

# INSERT your IP address here
app.run(host="192.168.1.202", port=5000, debug=True)

