import tensorflow as tf
import Layers

class ConvNeuralNet(tf.Module):
    def __init__(self):
        self.unflat = Layers.unflat('unflat',48, 48, 1) # from vector to table
        self.cv1 = Layers.conv('conv_1', output_dim=3, filterSize=3, stride=1) # conv2d
        # kernel: multiplication en decalage des pixels par des coefficients (ici kernel = w)
        # padding: ajout de coefficients avant/apres la matrice d'inputs
        # strides: coefficient de decalage a chaque multiplication

        self.mp = Layers.maxpool('pool', 2)
        # filtre qui prend le maximum des valeurs dans un voisinage de pixels

        self.cv2 = Layers.conv('conv_2', output_dim=6, filterSize=3, stride=1)
        self.cv3 = Layers.conv('conv_3', output_dim=12, filterSize=3, stride=1)
        self.flat = Layers.flat()
        self.fc = Layers.fc('fc', 2)

    def __call__(self, x, log_summary):
        x = self.unflat(x, log_summary)
        x = self.cv1(x, log_summary)
        x = self.mp(x)
        x = self.cv2(x, log_summary)
        x = self.mp(x)
        x = self.cv3(x, log_summary)
        x = self.mp(x)
        x = self.flat(x)
        x = self.fc(x, log_summary)
        return x