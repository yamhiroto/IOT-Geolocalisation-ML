import tensorflow as tf
import matplotlib.pyplot as plt
import numpy as np
from PIL import Image
import Model

IMG_PATH = '/home/savoga/Documents/Deep_Learning_Idemia/JR_48.jpg'
LoadModel = True

model = Model.ConvNeuralNet()

# ----- Load trained model -----
if LoadModel:
    ckpt = tf.train.Checkpoint(step=tf.Variable(1), optimizer=tf.optimizers.Adam(1e-3), net=model)
    ckpt.restore('./saved_model-1')

# ----- Load image and transform to array -----
img_gray = Image.open(IMG_PATH).convert('L')
img_arr = np.asarray(img_gray.getdata(band=None)) # transformation checked
img_norm = ((img_arr.reshape(len(img_arr),1)-128.0)/256.0).astype(np.float32)
X = img_norm

# ------ Get prediction for image -----
pred = np.asarray(tf.nn.sigmoid(model(img_norm, False)))
gender = "WOMAN" if np.argmax(pred) == 1 else "MAN"
print("Predicted gender: {}".format(gender))

plt.figure(0)
plt.imshow(X.reshape(48,48), cmap='gray')
plt.title("Initial image = {}".format(gender))
plt.savefig("initial_image.png")
plt.show()

print ("-----------------------------------------------------")
print ("-----------------------------------------------------")
print ("-----------------------------------------------------")

# ----- Change labels associated to image ------
gender_fake = "WOMAN" if gender=="MAN" else "MAN"
print("Fake gender: {}".format(gender_fake))

wrongLabel = np.array([1., 0.], dtype=np.float32)

DX = tf.Variable(np.zeros([2304,1]).astype(np.float32))
optimizer = tf.optimizers.Adam(1e-3)
nb_iter = 0

# ------ Train parameter DX until the network recognize another gender ------
while(gender_fake != gender):
    with tf.GradientTape() as tape:
        nb_iter += 1
        y = model(X+DX,False)
        y = tf.nn.log_softmax(y)
        diff = wrongLabel * y
        loss = -tf.reduce_sum(diff)+0.5*tf.nn.l2_loss(DX) # Ridge regularization to keep DX small
        grads = tape.gradient(loss, [DX])
        optimizer.apply_gradients(zip(grads, [DX]))
    pred=np.asarray(tf.nn.sigmoid(model(X+DX,False)))
    gender = "WOMAN" if np.argmax(pred) == 1 else "MAN"
    print("Predictions to be a man / woman: {}".format(pred))

# ------ Save figures ------

print("Gender is now {} after {} iterations".format(gender, nb_iter))
plt.figure(1)
plt.imshow((abs(DX.numpy())*50).reshape(48,48), cmap='gray')
plt.title("DX (disruptions)")
plt.savefig("disruptions.png")
plt.figure(2)
plt.imshow((DX.numpy()+X).reshape(48,48), cmap='gray')
plt.title("Transformed image = {}".format(gender))
plt.savefig("transformed_image.png")