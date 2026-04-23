# 🌍 Smart Travel Planner - TFG Ingeniería Informática

[cite_start]Aplicación móvil nativa para Android que centraliza la planificación de viajes y excursiones mediante el uso de Inteligencia Artificial[cite: 71, 74].

## 📝 Descripción del Proyecto
[cite_start]Este proyecto resuelve la fragmentación de información al organizar un viaje (Google Maps, Notas, Calendario, etc.) centralizando toda la experiencia en una única plataforma[cite: 77, 82]. [cite_start]La aplicación permite no solo gestionar la logística básica, sino también recibir recomendaciones inteligentes y generar itinerarios automáticos[cite: 75].

## ✨ Características Principales
* [cite_start]**Gestión de Viajes y Excursiones:** Creación, edición y visualización de viajes con destinos y fechas específicas[cite: 89, 90].
* [cite_start]**Itinerarios Inteligentes:** Organización de lugares por días con visualización en mapa[cite: 97].
* [cite_start]**Asistente de IA (OpenAI):** Chatbot que genera itinerarios automáticos y sugiere actividades personalizadas[cite: 98, 99].
* [cite_start]**Búsqueda de Lugares (Google Places):** Integración para encontrar hoteles, restaurantes y atracciones en tiempo real[cite: 93].
* [cite_start]**Colaboración:** Gestión de participantes con asignación de roles (organizador/participante)[cite: 101].
* [cite_start]**Favoritos:** Sistema para guardar y reutilizar lugares de interés[cite: 95].

## 🛠️ Stack Tecnológico
* [cite_start]**Lenguaje:** Java + XML (Android Nativo)[cite: 74, 134].
* [cite_start]**Backend:** Firebase (Cloud Firestore & Authentication)[cite: 124, 134].
* **APIs Externas:**
    * [cite_start]**OpenAI API:** Para la lógica de recomendación e itinerarios IA[cite: 116, 122].
    * [cite_start]**Google Places API:** Para el motor de búsqueda de lugares[cite: 121, 122].
    * [cite_start]**Google Maps SDK:** Para la visualización de rutas y mapas[cite: 122].

## 🏗️ Arquitectura de Datos
[cite_start]La aplicación utiliza una base de datos NoSQL (**Cloud Firestore**) con la siguiente estructura de colecciones[cite: 124, 125, 126, 130]:
* `Usuarios`: Perfiles, correos y preferencias.
* `Viajes`: Información principal del destino, fechas y participantes.
* `Excursiones`: Actividades de un día vinculadas a un viaje.
* `Itinerarios`: Organización cronológica de lugares.
* `Favoritos`: Referencias a lugares guardados por el usuario.

## 📅 Planificación y Metodología
[cite_start]El proyecto se ha desarrollado bajo una metodología **Agile/Scrum** dividida en 11 hitos principales[cite: 153, 155]:
1.  **Setup & Infrastructure:** Configuración de Firebase y APIs.
2.  **Authentication:** Sistema de registro y login.
3.  **Trip Management:** Ciclo de vida completo del viaje (CRUD).
4.  **AI & Search Integration:** Implementación de OpenAI y Google Places.
5.  **Testing & QA:** Pruebas de rendimiento y corrección de bugs.

## 🚀 Instalación y Configuración
1.  Clonar el repositorio: `git clone https://github.com/tu-usuario/nombre-repo.git`
2.  Abrir el proyecto en **Android Studio**.
3.  Vincular el proyecto con Firebase y añadir el archivo `google-services.json` en la carpeta `app/`.
4.  Configurar las API Keys de **OpenAI** y **Google Cloud** en el archivo `local.properties` o en los recursos correspondientes.

---
**Autor:** [Borja Ticona]  
**Grado:** Desarrollo de Aplicaciones Multiplataforma [cite: 72]  
