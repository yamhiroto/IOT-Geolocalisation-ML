# Pour une base de compréhension des convolutions
# https://riptutorial.com/tensorflow/example/30750/math-behind-1d-convolution-with-advanced-examples-in-tf

import tensorflow as tf
import DataSets as ds
import os
import Model
import matplotlib.pyplot as plt
import numpy as np
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

LoadModel = True
SaveModel = True
DisplayFewImages = True

DATA_PATH = '/home/savoga/Documents/Deep_Learning_Idemia'
experiment_size = 100
# filename_data, filename_gender, nbdata, batchSize=128
train = ds.DataSet(DATA_PATH + '/Databases/data_%dk.bin'%experiment_size, DATA_PATH + '/Databases/gender_%dk.bin'%experiment_size,1000*experiment_size)
test = ds.DataSet(DATA_PATH + '/Databases/data_test10k.bin', DATA_PATH + '/Databases/gender_test10k.bin',10000)

def train_one_iter(model, optimizer, image, label, log_summary):
    with tf.GradientTape() as tape:
        y = model(image,log_summary)
        y = tf.nn.log_softmax(y)
        diff = label * y
        loss = -tf.reduce_sum(diff)
        if log_summary:
            tf.summary.scalar('cross entropy', loss)
        grads = tape.gradient(loss, model.trainable_variables)
        optimizer.apply_gradients(zip(grads, model.trainable_variables))
    return loss

print ("-----------------------------------------------------")
print ("----------------------- %dk -------------------------"%experiment_size)
print ("-----------------------------------------------------")

train_summary_writer = tf.summary.create_file_writer('logs %dk'%experiment_size)
optimizer = tf.optimizers.Adam(1e-3) # learning rate = average of past gradients and average of past squared gradients
simple_cnn = Model.ConvNeuralNet()

if LoadModel:
    ckpt = tf.train.Checkpoint(step=tf.Variable(1), optimizer=optimizer, net=simple_cnn)
    ckpt.restore('./saved_model-1')

for iter in range(1):
    tf.summary.experimental.set_step(iter)
    if iter % 100 == 0:
        with train_summary_writer.as_default():
            acc1 = train.mean_accuracy(simple_cnn) * 100
            acc2 = test.mean_accuracy(simple_cnn) * 100
            print("iter= %6d accuracy - train= %.2f%% - test= %.2f%%" % (iter, acc1, acc2))
    ima, lab = train.NextTrainingBatch()
    with train_summary_writer.as_default():
        loss = train_one_iter(simple_cnn, optimizer, ima, lab, iter % 10 == 0)

    if iter % 100 == 0:
        print("iter= %6d - loss= %f" % (iter, loss))

if SaveModel:
    ckpt = tf.train.Checkpoint(step=tf.Variable(1), optimizer=optimizer, net=simple_cnn)
    ckpt.save('./saved_model')

if DisplayFewImages:
    for i in range(3):
        plt.imshow(np.asarray(ima[i]).reshape(48,48), cmap='gray') # second column is 1 if it's a woman