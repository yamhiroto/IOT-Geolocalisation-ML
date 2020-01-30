#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import pandas as pd
import numpy as np
from sklearn import tree
import os
import time

file_address = os.getcwd() + "/data/"
data_x_train = pd.read_csv(file_address + 'sp-explanatory-train.csv')
data_y_train = pd.read_csv(file_address + 'sp-response-train.csv')

data_x_test = pd.read_csv(file_address + 'sp-explanatory-valid.csv', header=None)
data_x_test.columns = data_x_train.columns.tolist()

def computeYields(df):
    df_sp_ret = df.T.apply(lambda x: (x/ x.shift(1, axis='rows')) - 1)
    df_sp_ret_transpose = df_sp_ret.T
    df_sp_ret_final = df_sp_ret_transpose.drop(df_sp_ret_transpose.columns[[0]],axis=1)
    return df_sp_ret_final

def features(df):

    df_sp = df.drop(['openGammaCall','openGammaPut'], axis=1)
    
    df_sp_ret = computeYields(df_sp)
    
    # Feature 1: total gamma position
    x_gamma = df.openGammaCall - df.openGammaPut
    x_gamma = x_gamma.to_frame()
    x_gamma.columns = ['gammaDiff']

    # Feature 2: S&P level difference open/close
    x_last = df_sp[df_sp.columns[[df_sp.shape[1]-1]]].apply(lambda x: x - np.ones(df_sp.shape[0])*100)
    x_last.columns = ['s&pDiff']

    # Feature 3: S&P yield variance
    x_var = df_sp_ret.T.apply(lambda x: np.var(x)).to_frame()
    x_var.columns = ['s&pVar']

    # Feature 4: S&P yield autocorrelation
    x_autocorr = df_sp_ret.T.apply(lambda x: x.autocorr(lag=10)).to_frame()
    x_autocorr.columns = ['s&pAutocorr']

    X = pd.concat([x_gamma, x_last, x_var, x_autocorr], axis=1)

    return X

X_train = features(data_x_train)
Y_train = data_y_train.Y.tolist()

X_test = features(data_x_test)

# Prediction

clf = tree.DecisionTreeClassifier(random_state = 0)
clf = clf.fit(X_train,Y_train)
Df_rest = pd.DataFrame (clf.predict(X_test), columns= ['Y'])

# ---- For visualisation results see the notebook