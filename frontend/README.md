# Panel Angular de MyEnterpriseOpenSource

Interfaz de prueba para las API del backend. Requiere Node.js 22 o posterior y pnpm 11.

Desde `frontend/`:

```powershell
pnpm install
pnpm start
```

Abre `http://127.0.0.1:4200`. El servidor Angular reenvía las peticiones `/api` a `http://127.0.0.1:8080` mediante `proxy.conf.json`. Inicia primero el backend desde la raíz del repositorio con `.\mvnw.cmd spring-boot:run` y Docker Desktop en ejecución.

Si `pnpm` no está disponible en otra máquina, usa `corepack pnpm install` y `corepack pnpm start` desde esta carpeta.

En el primer arranque local, el backend crea un administrador y guarda sus credenciales en `.local/credentials.txt`. El JWT se conserva durante la sesión de la pestaña y expira a los 15 minutos.

```powershell
pnpm build
pnpm test --watch=false
```

La compilación optimizada queda en `dist/frontend/`.
