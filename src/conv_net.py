#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jul 11 00:38:16 2022

@author: matheus
"""
from datetime import datetime

import h5py
import numpy as np

import torch
from torch import nn
from torch.nn import Module
from torch.nn import Sequential
from torch.nn import Conv2d
from torch.nn import LeakyReLU
from torch.nn import Flatten
from torch.nn import Linear
from torch.nn import Dropout
from torch.nn import LogSoftmax
from torch.utils.data import DataLoader
from torch.utils.data import Dataset
from torchvision import datasets
from torchvision.transforms import ToTensor

class H5_Dataset(Dataset):
    def __init__(self, hd5_file):
        super(Dataset, self).__init__()
        self.hd5_file = h5py.File(hd5_file)
        self.index = self.create_index()
        
    def __getitem__(self, i):
        return self.index[i]

    def __len__(self):
        return len(self.index)

    def create_index(self):
        data = []
        for i in range(len(self.hd5_file['data'])):
            d = self.hd5_file['data'][i]
            data.append(torch.from_numpy(d).type(torch.float))
        
        return list(
            zip (
                torch.stack(data), 
                torch.tensor(self.hd5_file['label'], dtype=torch.long)
            )
        )
        
                
class NeuralNetwork(nn.Module):
    def __init__(self):
        super(NeuralNetwork, self).__init__()
        self.flatten = nn.Flatten()
        self.linear_relu_stack = nn.Sequential(
            nn.Linear(8*8*25, 512),
            nn.ReLU(),
            nn.Linear(512, 512),
            nn.ReLU(),
            nn.Linear(512, 2)
        )

    def forward(self, x):
        x = self.flatten(x)
        logits = self.linear_relu_stack(x)
        return logits
    
class ConvNetwork(Module):
    def __init__(self):
        super(ConvNetwork, self).__init__()
        slope = -1 / 5.5
        self.stack = Sequential(
            # Conv1 64 3x3 padding 1
            Conv2d(in_channels=25, out_channels=64, kernel_size=3, padding=1),
            LeakyReLU(negative_slope=slope),
            # Conv2 32 3x3 padding 1
            Conv2d(in_channels=64, out_channels=32, kernel_size=3, padding=1),
            LeakyReLU(negative_slope=slope),
            # Conv3 1 1x1
            Conv2d(in_channels=32, out_channels=1, kernel_size=1),
            LeakyReLU(negative_slope=slope),
            
            Flatten(),
            # Fully Connected 1
            Linear(in_features=64, out_features=128),
            LeakyReLU(negative_slope=slope),
            Dropout(),
            # Fully Connected 2
            Linear(in_features=128, out_features=64),
            LeakyReLU(negative_slope=slope),
            Dropout(),            
            # Output
            Linear(in_features=64, out_features=2),
            LogSoftmax()
        )
        for layer in self.stack:
            nn.init.xavier_uniform_(layer.weight)
    
    def foward(self, x):
        return self.stack(x)
    

def train(dataloader, model, loss_fn, optimizer, training_steps):
    size = len(dataloader.dataset)
    lr = optimizer.param_groups[0]['lr']
    print(f'Learning rate = {lr}')
    model.train()
    for batch, (X, y) in enumerate(dataloader):
        X, y = X.to(device), y.to(device)

        # Compute prediction error
        pred = model(X)
        loss = loss_fn(pred, y)

        # Backpropagation
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()
        
        training_steps += 1
        if training_steps % 40000 == 0:
            for g in optimizer.param_groups:
                g['lr'] *= 0.2

        if batch % 100 == 0:
            loss, current = loss.item(), batch * len(X)
            print(f"loss: {loss:>7f}  [{current:>5d}/{size:>5d}]")
        
    return training_steps
            
def test(dataloader, model, loss_fn):
    size = len(dataloader.dataset)
    num_batches = len(dataloader)
    model.eval()
    test_loss, correct = 0, 0
    with torch.no_grad():
        for X, y in dataloader:
            X, y = X.to(device), y.to(device)
            pred = model(X)
            test_loss += loss_fn(pred, y).item()
            correct += (pred.argmax(1) == y).type(torch.float).sum().item()
    test_loss /= num_batches
    correct /= size
    print(f"Test Error: \n Accuracy: {(100*correct):>0.1f}%, Avg loss: {test_loss:>8f} \n")
    return (correct, test_loss)


# Get cpu or gpu device for training.
device = "cuda" if torch.cuda.is_available() else "cpu"
log_path = '../data/logs/'

def main():

    print("Starting ...")
    
    now = datetime.now()
    log = now.strftime('%d-%m %H-%M-%S') + '\n'
    
    trainFile = '../data/train-combined.h5'
    testFile = '../data/test-combined.h5'
    
    training_data = H5_Dataset(trainFile)
    test_data = H5_Dataset(testFile)
    
    batch_size = 64
    # Create data loaders.
    train_dataloader = DataLoader(training_data, batch_size=batch_size)
    test_dataloader = DataLoader(test_data, batch_size=batch_size)

    print(f"Using {device} device")
    # Define model
    model = NeuralNetwork().to(device)
    print(model)
    
    
    loss_fn = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=1e-5)
            
    acc = 0
    loss = 0
    training_steps = 0
    epochs = 160
    for t in range(epochs):
        print(f"Epoch {t+1}\n-------------------------------")
        training_steps = train(train_dataloader, model, loss_fn, optimizer, training_steps)
        acc, loss = test(test_dataloader, model, loss_fn)
    print("Done!")
    
    filename = '../data/models/model-' + now.strftime('%d-%m-%H-%M')
    torch.save(model.state_dict(), filename)
    
    log = log + 'Test samples = ' + str(len(test_dataloader.dataset)) + '\n'
    log = log + str('Acc = {:.2f}%,'.format(acc*100)) + str(' Avg loss: {:.8f}'.format(loss)) + '\n\n'
    
    test_dir = '../data/test/combined/'
    for i in range(20):
        testFile = test_dir + 'test' + str(i*5) + '-' + str((i+1)*5) + '.h5'
        test_data = H5_Dataset(testFile)
        test_dataloader = DataLoader(test_data, batch_size=batch_size)
        acc, loss = test(test_dataloader, model, loss_fn)
        log = log + 'Game lenght = ' + str(i*5) + '-' + str((i+1)*5) + '(' + str(len(test_dataloader.dataset)) + ')\n'
        log = log + str('Acc = {:.2f}%,'.format(acc*100)) + str(' Avg loss: {:.8f}'.format(loss)) + '\n\n'
    

    with open(log_path + 'log-' + now.strftime('%d-%m-%H-%M'), 'w') as f:
        f.write(log)
    
    
    print("Bye")


if __name__ == '__main__':
    main()