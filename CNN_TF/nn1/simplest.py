import tensorflow as tf
import numpy as np
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
from pathlib import Path


print("TensorFlow version: {}".format(tf.__version__))


# nombre d images
nbdata = 1000

path = Path(os.getcwd())
trainDataFile = str(Path(os.getcwd()).parent) + '/Databases/data_1k.bin'
LabelFile = str(Path(os.getcwd()).parent) + '/Databases/gender_1k.bin'

# taille des images 48*48 pixels en niveau de gris
dim = 2304
f = open(trainDataFile, 'rb')
data = np.empty([nbdata, dim], dtype=np.float32)
for i in range(nbdata):
	data[i, :] = np.fromfile(f, dtype=np.uint8, count=dim).astype(np.float32)
f.close()


f = open(LabelFile, 'rb')
label = np.empty([nbdata, 2], dtype=np.float32)
for i in range(nbdata):
	label[i, :] = np.fromfile(f, dtype=np.float32, count=2)
f.close()

# "framework" d'une couche (initialisation + formule de régression)
class fc_layer(tf.Module):
	def __init__(self, input_dim, output_dim):
        # Initialisation - 2304x50 (un poids par pixel et 50 couches cachées)
		w_init = tf.random.truncated_normal([input_dim, output_dim], stddev=0.1)
		self.w = tf.Variable(w_init) # Tenseur "variable" car il sera mis à jour
		print('w      ', self.w.get_shape())
		b_init = tf.constant(0.0, shape=[output_dim])
		self.b = tf.Variable(b_init) # Idem (attention b a la même valeur pour chaque pixel)
		print('b      ', self.b.get_shape())

	def __call__(self, x):
		return tf.matmul(x, self.w) + self.b

# construction du réseau (nombre de couches, fonctions d'activation)
class SimpleNet(tf.Module):
	def __init__(self, input_dim):
		self.fc1 = fc_layer(input_dim,50) # hidden layer
		self.fc2 = fc_layer(50,2) # output layer (proba 1 et proba 0 en sortie)

	def __call__(self, x):
		x = self.fc1(x)
		x = tf.nn.sigmoid(x)
		x = self.fc2(x)
		return x


def train_one_step(model, optimizer, image, label):
	with tf.GradientTape() as tape:
		y = model(image) # SimpleNet.call, fc_layer.call
        # --> calcul de la première couche, puis de la fonction d'activation,
        # puis de la deuxième couche
		loss = tf.reduce_sum(tf.square(y - label)) # calcul de l'erreur par les moindres carrés
		grads = tape.gradient(loss, model.trainable_variables) # calcul du gradient de la loss
		optimizer.apply_gradients(zip(grads, model.trainable_variables)) # mise à jour des paramètres
	return loss


optimizer = tf.optimizers.SGD(1e-5) # Stochastic gradient descent
simple_model = SimpleNet(dim)

curPos = 0
batchSize = 256 # nombre de données d'entraînement utilisées dans la SGD
# avant la mise à jour des paramètres

for it in range(5000):
	if curPos + batchSize > nbdata:
		curPos = 0
	loss = train_one_step(simple_model, optimizer,
						  data[curPos:curPos + batchSize, :], label[curPos:curPos + batchSize, :])

	curPos += batchSize
	if it % 100 == 0 or it < 10:
		print("it= %6d - loss= %f" % (it, loss.numpy()))

