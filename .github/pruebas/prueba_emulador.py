"""Prueba la app en un Android emulado: la usa como una persona y guarda capturas."""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PAQUETE = "com.memoriavoz.app"
CAPTURAS = "capturas"
os.makedirs(CAPTURAS, exist_ok=True)


def adb(*a, check=True):
    r = subprocess.run(["adb", *a], capture_output=True, text=True, timeout=120)
    if check and r.returncode:
        raise SystemExit(f"adb {a} fallo: {r.stderr}")
    return r.stdout


def captura(nombre):
    time.sleep(1.2)
    with open(f"{CAPTURAS}/{nombre}.png", "wb") as f:
        f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout)
    print("captura", nombre, flush=True)


def nodos():
    for _ in range(6):
        adb("shell", "uiautomator", "dump", "/sdcard/v.xml", check=False)
        x = adb("exec-out", "cat", "/sdcard/v.xml", check=False)
        if x.startswith("<?xml"):
            return list(ET.fromstring(x).iter("node"))
        time.sleep(1)
    raise SystemExit("no se pudo leer la pantalla")


def centro(n):
    a, b, c, d = map(int, re.findall(r"\d+", n.get("bounds")))
    return str((a + c) // 2), str((b + d) // 2)


def tocar(n):
    adb("shell", "input", "tap", *centro(n))
    time.sleep(1)


def buscar(texto):
    for n in nodos():
        if n.get("text") == texto or n.get("content-desc") == texto:
            return n
    return None


def tocar_texto(texto, espera=15):
    fin = time.time() + espera
    while time.time() < fin:
        n = buscar(texto)
        if n is not None:
            tocar(n)
            return
        time.sleep(1)
    captura("error_" + re.sub(r"\W", "_", texto))
    raise SystemExit(f"no aparecio: {texto}")


def agregar(frase, foto=None):
    tocar_texto("Prefiero escribirlo")
    if foto:
        captura(foto + "_escribir")
    campo = next(n for n in nodos() if n.get("class") == "android.widget.EditText")
    tocar(campo)
    adb("shell", "input", "text", frase.replace(" ", "%s"))
    time.sleep(1)
    tocar_texto("Continuar")
    time.sleep(1)
    if foto:
        captura(foto + "_revisar")
    tocar_texto("Guardar")
    time.sleep(3)


def fallos():
    return [l for l in adb("logcat", "-d", check=False).splitlines()
            if "FATAL EXCEPTION" in l or f"Process: {PAQUETE}" in l or "ANR in " + PAQUETE in l]


apk = sys.argv[1]
adb("install", "-r", "-t", apk)
for p in ["RECORD_AUDIO", "POST_NOTIFICATIONS"]:
    adb("shell", "pm", "grant", PAQUETE, "android.permission." + p, check=False)
adb("shell", "wm", "dismiss-keyguard", check=False)
adb("logcat", "-c", check=False)
adb("shell", "am", "start", "-W", "-n", f"{PAQUETE}/.MainActivity")
time.sleep(3)
captura("01_inicio_vacio")

agregar("Probar la app en 2 minutos", "02_rapido")
agregar("Reunion con Juan manana a las 3 de la tarde", "03_reunion")
agregar("Cada lunes a las 8 revisar inventario")
agregar("Pagar la luz el 25")
agregar("La clave del wifi es naranja", "04_nota")
captura("05_inicio_con_datos")
adb("shell", "input", "swipe", "500", "1700", "500", "500", "500")
captura("06_inicio_abajo")

adb("shell", "cmd", "statusbar", "expand-notifications", check=False)
adb("shell", "cmd", "statusbar", "collapse", check=False)
adb("shell", "input", "keyevent", "KEYCODE_SLEEP", check=False)

llego = False
fin = time.time() + 200
while time.time() < fin:
    if "Probar la app" in adb("shell", "dumpsys", "notification", "--noredact", check=False):
        llego = True
        break
    time.sleep(4)
print("notificacion llego:", llego, flush=True)
time.sleep(5)
captura("07_aviso_pantalla_completa")
actividad = adb("shell", "dumpsys", "activity", "activities", check=False)
print("pantalla de aviso visible:", "AlarmaActivity" in actividad, flush=True)
print("servicio de voz:", "VozService" in adb("shell", "dumpsys", "activity", "services", PAQUETE, check=False), flush=True)
adb("shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
time.sleep(2)
captura("08_aviso_despierto")
adb("shell", "cmd", "statusbar", "expand-notifications", check=False)
captura("09_notificacion")
adb("shell", "cmd", "statusbar", "collapse", check=False)
if buscar("Posponer 10 min") is not None:
    tocar_texto("Posponer 10 min")
time.sleep(2)

adb("shell", "cmd", "uimode", "night", "yes", check=False)
adb("shell", "am", "force-stop", PAQUETE)
adb("shell", "am", "start", "-W", "-n", f"{PAQUETE}/.MainActivity")
time.sleep(3)
captura("10_modo_oscuro")
adb("shell", "am", "start", "-W", "-a", "com.memoriavoz.app.NUEVA", "-n", f"{PAQUETE}/.MainActivity", check=False)
time.sleep(3)
captura("11_atajo_escuchando")

malos = fallos()
print("\n".join(malos))
with open(f"{CAPTURAS}/logcat.txt", "w") as f:
    f.write(adb("logcat", "-d", check=False))
if malos:
    raise SystemExit("La app se cerro con error")
if not llego:
    raise SystemExit("La notificacion no llego a tiempo")
print("PRUEBA OK")
