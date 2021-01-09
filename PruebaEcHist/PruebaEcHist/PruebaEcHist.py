#INICIO

import os.path
import cv2
from skimage import color, io
import numpy as np
import matplotlib.pyplot as plt
import json
from flask import Flask,request,Response
import uuid
import skfuzzy as fuzzy

#Funcion para obtener la ecualizacion de histograma de una imagen

def EcHist(img):
    Ima = np.uint8( color.rgb2gray(cv2.resize(img, (576, 432))) * 255 )
    [filas, columnas] = Ima.shape
    
    histn = np.zeros(256)
    for i in range(filas):
        for j in range(columnas):
            pos = Ima[i,j]
            histn[pos] = histn[pos] + 1
    probn = np.zeros(256)
    probn = histn / (filas * columnas)
    Hn = probn.cumsum()
    
    sal = np.zeros( (filas, columnas) )
    for i in range(filas):
        for j in range(columnas):
            pos = Ima[i,j]
            sal[i, j] = np.uint8( Hn[pos]*255 )
    sal = np.uint8(sal)
    
    #Se salva el archivo

    path_file = ('static/%s.jpg' %uuid.uuid4().hex)
    io.imsave(path_file, sal)
    return json.dumps(path_file) #Regresa el nombre de la imagen


# Funcion para obtener ecualizacion de histograma difuso

def EcHistDifuso(img):
    gris = np.uint8( color.rgb2gray(cv2.resize(img, (576, 432))) * 255 )
    fil, col = gris.shape
    
    pixel = np.linspace(0, 255, 256)
    oscuros = fuzzy.zmf(pixel, 25, 130) #Conjunto difuzo tipo Z, en el 25 empieza a caer, y cae hasta 130, luego es 0
    claros = fuzzy.smf(pixel, 130, 230)
    grises = fuzzy.gbellmf(pixel, 55, 3, 128) #Apertura de campana, pendiente de caída, centro
    
    s1 = 10 
    s2 = 128
    s3 = 245
    
    newg = np.zeros(256)
    for i in range(256):
        newg[i] = (oscuros[i]*s1 + grises[i]*s2 + claros[i]*s3) / (oscuros[i] + grises[i] + claros[i])
    
    ehf = np.zeros( [fil, col] )
    for ff in range(fil):
        for cc in range(col):
            valor = gris[ff,cc]
            ehf[ff, cc] = newg[valor]
    ehf = np.uint8(ehf)

    #Se salva el archivo

    path_file = ('static/%s.jpg' %uuid.uuid4().hex)
    io.imsave(path_file, ehf)
    return json.dumps(path_file) #Regresa el nombre de la imagen


# Funcion para aplicar correccion gamma

def CorrGamma(img):
    gris = np.uint8(color.rgb2gray(cv2.resize(img, (576, 432)))*255)
    grisd = np.double(gris)
    cg=gris.copy()
    [fil, col] = gris.shape
    
    # gamma=np.double(input('Valor de gamma: '))
    gamma = 2
    k=255/np.max(grisd**gamma)
    
    for i in range(0, fil):
        for j in range(0, col):
            cg[i,j]=k*grisd[i,j]**(gamma)
    
    cg = np.uint8(cg)

    #Se salva el archivo

    path_file = ('static/%s.jpg' %uuid.uuid4().hex)
    io.imsave(path_file, cg)
    return json.dumps(path_file) #Regresa el nombre de la imagen


# Funcion para aplicar transformacion lineal

def TLineal(img):
    gris = np.uint8(color.rgb2gray(cv2.resize(img, (576, 432)))*255)
    grisd = np.double(gris)
    tl=gris.copy()
    [fil, col] = gris.shape
    
    # r1 = np.double(input('Ingrese r1: '))
    # s1 = np.double(input('Ingrese s1: '))
    # r2 = np.double(input('Ingrese r2: '))
    # s2 = np.double(input('Ingrese s2: '))
    r1 = 100
    s1 = 100
    r2 = 255
    s2 = 150
    
    for i in range(fil):
        for j in range(col):
            if gris[i, j] <= r1:
                tl[i, j] = (s1/r1)*grisd[i, j]
            elif gris[i, j] > r1 and gris[i, j] <= r2:
                tl[i, j] = ((s2-s1)/(r2-r1))*(grisd[i, j]-r1)+s1
            else:
                tl[i, j] = ((255-s2)/(255-r2))*(grisd[i, j]-r2)+s2

    #Se salva el archivo

    path_file = ('static/%s.jpg' %uuid.uuid4().hex)
    io.imsave(path_file, tl)
    return json.dumps(path_file) #Regresa el nombre de la imagen


# Funcion para aplicar High Mask Filter

def HMF(img):
    img=cv2.cvtColor(cv2.resize(img, (576, 432)), cv2.COLOR_BGR2GRAY)
    
    dft=cv2.dft(np.float32(img),flags=cv2.DFT_COMPLEX_OUTPUT)
    dft_shift=np.fft.fftshift(dft)
    magnitude_spectrum=20*np.log(cv2.magnitude(dft_shift[:,:,0],dft_shift[:,:,0])+1)
    
    #Filtro
    rows,cols=img.shape
    crow,ccol=int(rows/2),int(cols/2)
    mask=np.ones((rows,cols,2),np.uint8)
    r=3
    center=[crow,ccol]
    x,y=np.ogrid[:rows,:cols]
    mask_area=(x-center[0])**2+(y-center[1])**2<=r*r
    mask[mask_area]=0 
    fshift=dft_shift*mask
    fshift_mask_mag=2000*np.log(cv2.magnitude(fshift[:,:,0],fshift[:,:,1]))
    f_ishift=np.fft.ifftshift(fshift)
    img_back=cv2.idft(f_ishift)
    img_back=cv2.magnitude(img_back[:,:,0],img_back[:,:,1])

    #Se salva el archivo

    path_file = ('static/%s.jpg' %uuid.uuid4().hex)
    io.imsave(path_file, img_back)
    return json.dumps(path_file) #Regresa el nombre de la imagen


#Funcion para aplicar Sobel

def Sobel(img):
    gris = np.uint8(color.rgb2gray(cv2.resize(img, (576, 432)))*255) 
    
    [filas, columnas]= gris.shape
    #n será el factor por el que se multiplicarán pixeles adyacentes
    #considerando el comentario hecho en clase sobre si al aumentarlo
    #se lograba suavizar más la imagen
    n=2
    
    #Se aplica la máscara de Sobel a la imagen
    Sobel = np.zeros(gris.shape)
    dfx=np.zeros(gris.shape)
    dfy=np.zeros(gris.shape)
    for x in range(filas - 3):
        for y in range(columnas - 3):
            dfx[x+2,y+2]=abs(gris[3+x,1+y]+n*gris[3+x,2+y]+gris[3+x,3+y]-(gris[1+x,1+y]+n*gris[1+x,2+y]+gris[1+x,3+y]))
            if dfx[x+2,y+2]>255:
                dfx[x+2,y+2]=255
            else:
                dfx[x+2,y+2]=dfx[x+2,y+2]
            dfy[x+2,y+2]=abs(gris[1+x,3+y]+n*gris[2+x,3+y]+gris[3+x,3+y]-(gris[1+x,1+y]+n*gris[2+x,1+y]+gris[3+x,1+y]))        
            if dfy[x+2,y+2]>255:
                dfy[x+2,y+2]=255
            else:
                dfy[x+2,y+2]=dfy[x+2,y+2]
            Sobel[x+2,y+2]=dfx[x+2,y+2]+dfy[x+2,y+2]
    
    #Se ingresa el valor del umbral seleccionado
    # umbral=int(input("Umbral para recalcar el borde: "))
    umbral = 9
    
    #Se realiza la suma del 60% de la imagen original + 40% de la imagen obtenida
    #con Sobel si el valor de Sobel es menor al umbral seleccionado
    #Si el valor es mayor al umbral, se respeta el valor de Sobel para recalcar el borde
    Ima = np.zeros(gris.shape)
    for i in range(filas):
        for j in range(columnas):
            if Sobel[i,j]<umbral:
                Ima[i,j]=0.4*+Sobel[i,j]+0.6*gris[i,j]
            else:
                Ima[i,j]=Sobel[i,j]
    Ima=np.uint8(Ima)

    #Se salva el archivo

    path_file = ('static/%s.jpg' %uuid.uuid4().hex)
    io.imsave(path_file, Ima)
    return json.dumps(path_file) #Regresa el nombre de la imagen


#API

app = Flask(__name__)

#Ruta http post a este metodo

@app.route('/api/upload', methods=['GET','POST'])
def upload():
    #Recupera la imagen del cliente
    selec = np.int(request.form['selector'])
    print("num: ",selec)
    img = cv2.imdecode(np.fromstring(request.files['image'].read(), np.uint8),cv2.IMREAD_UNCHANGED)
    print("imagen:", img)    
    #Procesa la imagen
    if selec == 1:
        img_processed = EcHist(img)
    elif selec == 2:
        img_processed = EcHistDifuso(img)
    elif selec == 3:
        img_processed = TLineal(img)
    elif selec == 4:
        img_processed = Sobel(img)
    elif selec == 5:
        img_processed = CorrGamma(img)
    elif selec == 6:
        img_processed = HMF(img)
    #Resultado
    return Response(response=img_processed,status=200,mimetype="application/json") #Devuelve un json string

#Se inicia el servidor

#app.run(host="0.0.0.0",port=5000)
app.run(host="192.168.1.72",port=5000)