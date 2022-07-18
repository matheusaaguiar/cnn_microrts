#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Nov 23 19:59:42 2021

@author: matheus
"""

import h5py, os

import sys
import numpy as np

def rotateFPS(featurePlaneStack):
    
    rotatedFPS = np.empty_like(featurePlaneStack)
    for i, fp in enumerate(featurePlaneStack):
        rotatedFPS[i] = np.rot90(fp)
    
    return rotatedFPS

def reflectFPS(featurePlaneStack, direction=None):
    
    reflectedFPS = np.empty_like(featurePlaneStack)
    for i, fp in enumerate(featurePlaneStack):
        reflectedFPS[i] = np.flip(fp, direction)
    
    return reflectedFPS
    
def getFPSRotations(featurePlaneStack):
    
    k, h, w = featurePlaneStack.shape
    rotations = np.empty( (3,  k, h, w) )
    
    rotations[0] = rotateFPS(featurePlaneStack)
    rotations[1] = rotateFPS(rotations[0])
    rotations[2] = rotateFPS(rotations[1])
    
    return rotations
        
def getFPSReflections(featurePlaneStack):
    
    k, h, w = featurePlaneStack.shape
    reflections = np.empty( (3, k, h, w) )
    
    reflections[0] = reflectFPS(featurePlaneStack, 0)  #HORIZONTAL
    reflections[1] = reflectFPS(featurePlaneStack, 1)  #VERTICAL
    reflections[2] = reflectFPS(featurePlaneStack)     #BOTH
    
    return reflections

def getColorReflection(featurePlaneStack):
    #Defined by the java counterpart
    feature_plane_id_owner0 = 11
    feature_plane_id_owner1 = 12

    rcolor = np.copy(featurePlaneStack)
    
    aux = np.copy(rcolor[feature_plane_id_owner0])
    rcolor[feature_plane_id_owner0] = rcolor[feature_plane_id_owner1]
    rcolor[feature_plane_id_owner1] = aux

    return rcolor

def readFPSfromFile(f):
    
    with open(os.path.join(samples_dir, f), 'r') as F:   #open file and read lines
        lines = F.readlines()
    
    fields = lines[0].split(' ')        #read first line header fields
    nPlanes = int(fields[0])            #number of feature planes in the stack
    planes_height = int( fields[1] )    #height of each feature plane
    planes_width = int( fields[2] )     #width of each feature plane
    winner = int(fields[3])             #label associated with stack of feature planes
    label = np.float32( winner )
    game_time_percentage = int(fields[4])
 
    #Build stack from subsequent lines
    feature_planes_stack = np.empty( (nPlanes, planes_height, planes_width), dtype=np.float32 )
    for j, line in enumerate(lines[1:]):
        sp = line.split(' ')
        sp.remove('\n')
        fp = np.array(sp, dtype=np.float32)
        fp = fp.reshape( (planes_height, planes_width) )
        feature_planes_stack[j, ...] = fp
        
    return feature_planes_stack, label, game_time_percentage


log = ''
samples_dir_list = ['/home/matheus/doutorado/pytorch/data/samples/tournament-200ms/',
               '/home/matheus/doutorado/pytorch/data/samples/tournament-100ms/'
               ]
samples_files = []
for samples_dir in samples_dir_list:
    log = log + 'Reading files from' + samples_dir + '\n'
    print("Reading files from " + samples_dir)

    for f in os.listdir(samples_dir):
        if f != 'statistics.txt':
            samples_files.append(f)

    #if('statistics.txt' in samples_files):
     #   samples_files.remove('statistics.txt')


import random
random.shuffle(samples_files)  #shuffle file names to randomize data sequence
nSamples = len(samples_files)

print("Processing " + str(nSamples) + " Samples")
log = log + "Processing " + str(nSamples) + " Samples" + '\n'

trainingSize = 0.8
nTraining = int(nSamples * trainingSize)
nTest = nSamples - nTraining

trainingFiles = samples_files[0:nTraining]
testFiles = samples_files[nTraining:]

print("Reserved " + str(len(trainingFiles)) + " for training")

print("Reserved " + str(len(testFiles)) + " for testing")


X = np.empty( (nTraining*8, 25, 8, 8), dtype=np.float32 ) 
y = np.empty( (nTraining*8, ), dtype=np.float32 )
i = 0
for f in trainingFiles:
    feature_plane_stack, label, gtime = readFPSfromFile(f)
    X[i] = feature_plane_stack
    y[i] = label
    i += 1
    for rt in getFPSRotations(feature_plane_stack):
        X[i] = rt
        y[i] = label
        i += 1
    for rf in getFPSReflections(feature_plane_stack):  #3 pos reflections
        X[i] = rf
        y[i] = label
        i += 1
    X[i] = getColorReflection(feature_plane_stack)
    y[i] = (label + 1) % 2 #invert winner
    i += 1
    
    
Xt = np.empty( (nTest, 25, 8, 8), dtype=np.float32 ) 
yt = np.empty( (nTest, ), dtype=np.float32 )
#z = np.empty( (nTest, ), dtype=np.int32 )
Gtime = [ [] for i in range(20)]
for i, f in enumerate(testFiles):
    feature_plane_stack, label, gtime = readFPSfromFile(f)
    Xt[i] = feature_plane_stack
    yt[i] = label
    j = int(gtime) // int(5)
    if j == 20:
        j -= 1
    Gtime[j].append( (Xt[i], yt[i]) )
#    z[i] = j
        
    
print("Augmenting training data with rotations (4x)")
log = log + 'Augmenting training data with rotations(4x)\n'

        

n = len(X) + len(Xt)
print(str(n) + ' feature planes created in total')
log = log + str(n) + ' feature planes created in total\n'
log = log + '\tTraining data: ' + str( len(X) ) + '\n'
log = log + '\tTest data: ' + str( len(Xt) )  + '\n'

   
print('\tTraining data: ' + str( len(X) ) )
print('\tTest data: ' + str( len(Xt) ) )
print('\tLabels : ' + str( len(y) ) )

print("Writting to HDF5 File")

dirname = os.path.abspath('/home/matheus/doutorado/pytorch/data/')
if not os.path.exists(dirname):
    os.makedirs(dirname)

train_filename = os.path.join(dirname, 'train-combined.h5')
test_filename = os.path.join(dirname, 'test-combined.h5')

with h5py.File( train_filename,'w') as H:
    H.create_dataset( 'data', data=X )    
    H.create_dataset( 'label', data=y )

with open(os.path.join(dirname, 'train.txt'), 'w') as f:
    f.write(train_filename + '\n')
    
with h5py.File( test_filename,'w') as H:
    H.create_dataset( 'data', data=Xt )    
    H.create_dataset( 'label', data=yt )
#    H.create_dataset( 'time', data=z )

with open(os.path.join(dirname, 'test.txt'), 'w') as f:
    f.write(test_filename + '\n')
    

dirname = os.path.abspath('/home/matheus/doutorado/pytorch/data/test/combined')

log = log + 'Test data sorted by game time percentage:' + '\n'
print('Test data sorted by game time percentage:')
for i in range(20):
    data_set = Gtime[i]
    Wt = np.empty( (len(data_set), 25, 8, 8), dtype=np.float32 ) 
    zt = np.empty( (len(data_set),), dtype=np.float32 ) 
    for j, (w, z) in enumerate(data_set):
        Wt[j] = w
        zt[j] = z
    
    log = log + '\t' + str(i*5) + ' - ' + str((i+1)*5) + ': ' + str(len(data_set)) + '\n'
    print('\t' + str(i*5) + ' - ' + str((i+1)*5) + ': ' + str(len(data_set)))
    print('writting to HDF5 file...')
    test_name = os.path.join(dirname, 'test' + str(i*5) + '-' + str((i+1)*5) + '.h5')
    with h5py.File(test_name, 'w') as H:
        H.create_dataset('data', data = Wt)
        H.create_dataset( 'label', data=zt )
    with open(os.path.join(dirname, 'test' + str(i*5) + '-' + str((i+1)*5) + '.txt'), 'w') as f:
        f.write(test_name + '\n')


with open(os.path.join('../data', 'database_generation_log.txt'), 'w') as f:
    f.write(log)

print("Finished")         
