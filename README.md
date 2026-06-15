# Smart Travel Planner - TFG Ingeniería Informática

Aplicación móvil nativa para Android que centraliza la planificación de viajes y excursiones mediante el uso de Inteligencia Artificial.

## Descripción del Proyecto

Este proyecto resuelve la fragmentación de información al organizar un viaje (Google Maps, Notas, Calendario, etc.) centralizando toda la experiencia en una única plataforma. La aplicación permite no solo gestionar la logística básica, sino también recibir recomendaciones inteligentes y generar itinerarios automáticos.

## Características Principales

* **Gestión de Viajes y Excursiones:** Creación, edición y visualización de viajes con destinos y fechas específicas.
* **Itinerarios Inteligentes:** Organización de lugares por días con visualización en mapa.
* **Asistente de IA (OpenAI):** Chatbot que genera itinerarios automáticos y sugiere actividades personalizadas.
* **Búsqueda de Lugares (Nominatim/OpenStreetMap):** Integración para encontrar hoteles, restaurantes y atracciones en tiempo real.
* **Favoritos:** Sistema para guardar y reutilizar lugares de interés.

## Stack Tecnológico

* **Lenguaje:** Java + XML (Android Nativo).
* **Backend:** Firebase (Cloud Firestore & Authentication).
* **APIs Externas:**
  * **OpenAI API:** Para la lógica de recomendación e itinerarios IA.
  * **Nominatim (OpenStreetMap):** Para el motor de búsqueda de lugares.
  * **Unsplash API:** Para la carga automática de imágenes de destinos.

## Arquitectura de Datos

La aplicación utiliza una base de datos NoSQL (**Cloud Firestore**) con la siguiente estructura de colecciones:

* `Usuarios`: Perfiles, correos y preferencias.
* `Viajes`: Información principal del destino, fechas y participantes.
* `Excursiones`: Actividades vinculadas a un viaje.
* `Itinerarios`: Organización cronológica de lugares generados por IA.
* `Favoritos`: Referencias a lugares guardados por el usuario.

## Planificación y Metodología

El proyecto se ha desarrollado bajo una metodología **Agile/Scrum** dividida en 11 hitos principales:

1. **Setup & Infrastructure:** Configuración de Firebase y APIs.
2. **Authentication:** Sistema de registro y login.
3. **Trip Management:** Ciclo de vida completo del viaje (CRUD).
4. **AI & Search Integration:** Implementación de OpenAI y Nominatim.
5. **Testing & QA:** Pruebas de rendimiento y corrección de bugs.

## Instalación y Configuración

1. Clonar el repositorio: `git clone https://github.com/tu-usuario/nombre-repo.git`
2. Abrir el proyecto en **Android Studio**.
3. Vincular el proyecto con Firebase y añadir el archivo `google-services.json` en la carpeta `app/`.
4. Configurar las API Keys de **OpenAI** y **Unsplash** en el archivo `local.properties`.

---

**Autor:** Borja Ticona  
**Grado:** Desarrollo de Aplicaciones Multiplataforma
