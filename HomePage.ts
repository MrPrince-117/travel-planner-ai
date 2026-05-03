import { Plus, Search, Bot } from 'lucide-react';
import { useNavigate } from 'react-router';
import { StatsCard } from '../components/StatsCard';
import { HorizontalTripCard } from '../components/HorizontalTripCard';
import { QuickActionButton } from '../components/QuickActionButton';
import { ActivityItem } from '../components/ActivityItem';

const upcomingTrips = [
  {
    id: 1,
    destination: 'París, Francia',
    dates: '15-22 Abr',
    imageUrl: 'https://images.unsplash.com/photo-1642947392578-b37fbd9a4d45?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxQYXJpcyUyMEVpZmZlbCUyMFRvd2VyJTIwc3Vuc2V0fGVufDF8fHx8MTc3NTQ2Njk2Mnww&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: 2,
    destination: 'Tokio, Japón',
    dates: '10-18 May',
    imageUrl: 'https://images.unsplash.com/photo-1648871647634-0c99b483cb63?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwyfHxUb2t5byUyMHNreWxpbmUlMjBuaWdodHxlbnwxfHx8fDE3NzU0NjY5NjJ8MA&ixlib=rb-4.1.0&q=80&w=1080',
  },
];

const recentActivity = [
  {
    id: 1,
    title: 'Ruta Gastronómica Roma',
    subtitle: 'Plan generado por IA',
    date: 'Hace 2 días',
    imageUrl: 'https://images.unsplash.com/photo-1552832230-c0197dd311b5?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxSb21lJTIwQ29saXNzZXVtfGVufDF8fHx8MTc3NTQ2Njk2M3ww&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: 2,
    title: 'Hoteles en Kioto',
    subtitle: 'Búsqueda guardada',
    date: 'Hace 5 días',
    imageUrl: 'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxLeW90byUyMHRlbXBsZXxlbnwxfHx8fDE3NzU0NjY5NjN8MA&ixlib=rb-4.1.0&q=80&w=1080',
  },
];

export function HomePage() {
  const navigate = useNavigate();

  return (
    <div className= "flex-1 overflow-y-auto bg-gray-50 pb-20" >
    {/* Stats Section */ }
    < div className = "px-4 py-6 bg-white border-b-4 border-black" >
      <h1 className="text-2xl font-black uppercase tracking-tight mb-4" > Mi Panel </h1>
        < div className = "grid grid-cols-2 gap-4" >
          <StatsCard label="Presupuesto Total" value = "2.450 €" color = "yellow" />
            <StatsCard label="Días de Viaje" value = "12" color = "pink" />
              </div>
              </div>

  {/* Upcoming Trips Section */ }
  <div className="px-4 py-8" >
    <div className="flex items-center justify-between mb-4" >
      <h2 className="font-bold text-xl text-gray-900 uppercase tracking-wide" > Próximos viajes </h2>
        < button className = "text-sm font-bold text-blue-600 underline" > Ver todos </button>
          </div>
          < div className = "flex gap-4 overflow-x-auto pb-4 -mx-4 px-4 scrollbar-hide" >
          {
            upcomingTrips.map((trip) => (
              <HorizontalTripCard
              key= { trip.id }
              destination = { trip.destination }
              dates = { trip.dates }
              imageUrl = { trip.imageUrl }
              />
          ))
          }
            </div>
            </div>

  {/* Quick Actions Section */ }
  <div className="px-4 py-5 bg-white" >
    <h2 className="font-semibold text-lg text-gray-900 mb-4" > Acciones rápidas </h2>
      < div className = "flex gap-3" >
        <QuickActionButton
            icon={ Plus }
  label = "Crear viaje"
  color = "blue"
  onClick = {() => navigate('/create-trip')
}
          />
  < QuickActionButton
icon = { Search }
label = "Buscar"
color = "green"
onClick = {() => navigate('/search')}
          />
  < QuickActionButton
icon = { Bot }
label = "Pedir a IA"
color = "purple"
onClick = {() => navigate('/chat')}
          />
  </div>
  </div>

{/* Recent Activity Section */ }
<div className="px-4 pt-6 pb-4" >
  <div className="mb-4" >
    <h2 className="font-semibold text-lg text-gray-900" > Actividad reciente </h2>
      < p className = "text-sm text-gray-600" > Tus planes guardados </p>
        </div>
        < div className = "space-y-3" >
        {
          recentActivity.map((activity) => (
            <ActivityItem
              key= { activity.id }
              title = { activity.title }
              subtitle = { activity.subtitle }
              date = { activity.date }
              imageUrl = { activity.imageUrl }
            />
          ))
        }
          </div>
          </div>
          </div>
  );
}