import torch
import torch.nn as nn
import pickle
import matplotlib.pyplot as plt
import torch.nn.functional as F
import torch.nn
import numpy as np
from torchvision import models
 
no_top_features = 5

# SETTING UP RESNET
def read_classes(class_path):
    with open(class_path) as f:
        return [line.strip() for line in f.readlines()]

def read_model(model_path):
    checkpoint = torch.load(model_path, map_location=torch.device('cpu')) # seems to work despite warning, returns a dict
    classes = checkpoint["classes"]
    resnet = models.resnext101_32x8d()
    x = checkpoint.get('state_dict')

    #remove 'module' string from key, necessary for CPU based model
    from collections import OrderedDict
    new_state_dict = OrderedDict()
    for k, v in x.items():
        name = k[7:] # remove `module.`
        new_state_dict[name] = v

    # load params
    #set number of classes
    num_ftrs = resnet.fc.in_features
    resnet.fc = torch.nn.Linear(num_ftrs, len(classes))
    resnet.load_state_dict(new_state_dict)
    resnet.eval()
    return classes, resnet


# if we wanted to print the names
#INSERT link to local labels_words.txt here
with open('/Users/kimschild/_ThesisFolder/ResNeXt-101/labels_words.txt') as f:
    classes_in_words = [line.strip() for line in f.readlines()]

#INSERT link to local model_best.pth here
path = '/Users/kimschild/_ThesisFolder/ResNeXt-101/model_best.pth'
checkpoint = torch.load(path, map_location=torch.device('cpu')) # seems to work despite warning, returns a dict
classes = checkpoint["classes"]
resnet = models.resnext101_32x8d()
x = checkpoint.get('state_dict')


#remove module from key
from collections import OrderedDict
new_state_dict = OrderedDict()
for k, v in x.items():
    name = k[7:] # remove `module.`
    new_state_dict[name] = v
# load params

#set number of classes
num_ftrs = resnet.fc.in_features
resnet.fc = torch.nn.Linear(num_ftrs, len(classes))
resnet.load_state_dict(new_state_dict)

resnet.eval()

# APPLY RESNET TO IMAGES

from PIL import Image
import glob
from scandir import scandir, walk

# transformations
from torchvision import transforms
# transformations
transform = transforms.Compose([
    transforms.Resize(256),  
    transforms.CenterCrop(224),  
    transforms.ToTensor(),
    transforms.Normalize(
        # mean=[0.5, 0.5, 0.5],
        # std=[0.5, 0.5, 0.5]
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )
    ])
idx_labels_with_n = list()
label_words_list = list()

def load_n_Label_to_Idx():
    #INSERT link to local conceptsRollBindPromoteSubsampleNonLeaf.txt here
    with open('/Users/kimschild/_ThesisFolder/ResNeXt-101/conceptsRollBindPromoteSubsampleNonLeaf.txt', 'r') as f:
        all_lines = f.readlines()
    for n_number in all_lines:
        new = n_number.strip()
        idx_labels_with_n.append(new)
    print(len(idx_labels_with_n))

def load_word_Label_List():
    with open('conceptList.txt', 'r') as labels:
        all_lines = labels.readlines()
    for label_name in all_lines:
        label_words_list.append(label_name)

def get_ResNext_idx_for_nNumber(n_number):
    return idx_labels_with_n.index(n_number)

def get_label_word_from_idx(idx):
    return label_words_list[idx]

def load_Images_From_Folder(path):
    image_list = []
    for image in scandir(path) :
        # conversion from pngs to jpgs because code was crashing on png
        im = Image.open(path + '/' + image.name)
        image_list.append(im)
    return image_list

def make_image(img):
    print('make_image was called')
    img_t = transform(img)
    batch_t = torch.unsqueeze(img_t, 0)
    out = resnet(batch_t)   
    #return out
    prob = torch.nn.functional.softmax(out, dim=1)
    return prob

#returns the resNext indexes (instead of the pytorch indexes) and their probs sorted according to labels 
def format_tensors(loopEnd, bothdimension_tensor):
    value_tensor, indexes_tensor = bothdimension_tensor
    indexes_list = indexes_tensor.squeeze(0).tolist()
    value_list = value_tensor.squeeze(0).tolist()   
    indexes_list = get_ResNext_idx_for_pytorch_idx(indexes_list) 
    print('we have  resnext indexes list.', indexes_list)
    print('old value order: ', value_list)
    #let the sorting start!
    sorted_value_list = [x for _,x in sorted(zip(indexes_list,value_list))]
    indexes_list.sort()
    sorted_value_list_ints = [int((i * 1000)) for i in sorted_value_list]
    print('sorted indexes lsit', indexes_list)
    print('sorted values list: ', sorted_value_list_ints)
    # get the labels in words
    label_list_words = list()
    for idx in indexes_list:
        label_word = get_label_word_from_idx(idx)
        stripped_word = label_word.strip()
        label_list_words.append(stripped_word)
    return indexes_list, sorted_value_list_ints

def get_ResNext_idx_for_pytorch_idx(indexes_list_old):
    tmp_vector = list()
    for idx in range(no_top_features):
        n_number = classes[indexes_list_old[idx]]
        resnext_idx = get_ResNext_idx_for_nNumber(n_number)
        tmp_vector.append(resnext_idx)
    return tmp_vector

def printFormatteddTensors(loopEnd, bothdimension_tensor):
    print('printFormatteddTensors was called')
    value_tensor = bothdimension_tensor[0] # values tensor in percentage
    indexes_tensor = bothdimension_tensor[1] # indices tensor
    resnext_indexes_tensor = torch.empty(1, no_top_features)
    tmp_vector = list()
    for idx in range(no_top_features):
        n_number = classes[indexes_tensor[idx]]
        resnext_idx = get_ResNext_idx_for_nNumber(n_number)
        tmp_vector.append(resnext_idx)
    data_input = [tmp_vector]
    resnext_indexes_tensor.new_tensor(data_input)
    sorted_idx, new_idx = torch.sort(resnext_indexes_tensor, dim=-1) #sort indices 
    sorted_values = torch.gather(value_tensor, dim=-1, index=new_idx) # sort values according to index
    v = sorted_values.data.numpy()
    i = sorted_idx.data.numpy()
    print('printFormatteddTensors finished')
    return (i.tolist()[0], v.tolist()[0])

## get images and apply image analysis
vector_collection = list()

#returns a string vector label:prob, etc
def apply_model(img, path):
    print('apply_model was called')
    out = make_image(img)
    predicted_k_indexes  = torch.topk(out, k= no_top_features) #sort according to labels
    labels, probs = format_tensors(no_top_features, predicted_k_indexes)
    data = {'path': path, 'labels': labels, 'probabilities': probs}
    print(path, labels, probs)
    print('analysed a pic')
    print('apply_model finished')
    return data





