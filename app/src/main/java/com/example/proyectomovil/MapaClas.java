package com.example.proyectomovil;

import android.location.Location;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;


import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapaClas extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener,
        PermissionsListener, MapboxMap.OnMapClickListener {

    private MapView mapView;
    Double lon;
    Double lac;

    //Creo variable el mapa para los usuarios
    private MapboxMap map;

    //Creo la variable para nuesto boto de inicio de ruta
    private Button startButton;

    //Creo variable los permisos del admin
    private PermissionsManager permissionsManager;

    //Creo variable para el motor de localizacion
    private LocationEngine locationEngine;

    //Creo variable capa de localizacion plugin
    private LocationLayerPlugin locationLayerPlugin;

    //Creo vriable para almacenar localizacion actual
    private Location originLocation;

    //Creo variable para almacenar la posicion actual del punto
    private Point originPosition;

    //Creo una variable donde almacenare la posicion de destino
    private Point destinationPosition;

    //Creo la varaible para crear el marcador de destino
    private Marker destinationMarker;

    //Creo una variable paa crear la ruta de navegacion
    private NavigationMapRoute navigationMapRoute;

    //Creo una etiqueta para alguna actividad mas tarde
    private static final String TAG ="MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this,getString(R.string.access_token));
        setContentView(R.layout.activity_mapa_clas);

        //Intaciamos mapView
        mapView =(MapView)findViewById(R.id.mapView);
        //Iniciamos el botonStart
        startButton = (Button)findViewById(R.id.startButton);
        //Creamos la instancia del mapView
        mapView.onCreate(savedInstanceState);
        //Obtenemos el mapa de forma asincrona
        mapView.getMapAsync(this);


        //Cuando le doy click al botonStart le digo que me corra la siguiente funcion
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Lanceme a la UI de navegacion
                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .origin(originPosition)
                        .destination(destinationPosition)
                        .shouldSimulateRoute(false)
                        .build();
                NavigationLauncher.startNavigation(MapaClas.this,options);
            }
        });
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        lon = Double.parseDouble(getIntent().getStringExtra("longitud"));
        lac=Double.parseDouble(getIntent().getStringExtra("lactitud"));
        mapboxMap.addMarker(new MarkerOptions().position(new LatLng(lac,lon))
                .title("UAO")
                .snippet("Facultad ingenieria"));

     // map.addOnMapClickListener(this);
       // enableLocation();
    }

    //Funcion para habilitar la localizacion
    private void enableLocation(){
        //Decimos si el usuario ha dado permisos inicieme mi ubicacion
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
            initializeLocationLayer();
            //Sino que pida los permisos de ubicacion
        }else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    //################
    //Despues de dar todos los permisos cedo a mi ubicacion
    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine(){
        //Creamos la instancia de nuestro motor de localizacion
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        //Asignamos alta presicion para nuestra localizacion
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        //Activamos nuestro motro de localizacion
        locationEngine.activate();

        //Tomamos nuestra localizacion anterior
        Location lastLocation = locationEngine.getLastLocation();
        //Decimos que si nuestra ubicacion es diferente a vacio entonce me asigne
        //mi localizacion anterior a mi variable originLocation
        if(lastLocation != null){
            originLocation = lastLocation;
            //Llamamos la funcion que anima la camara con la ubicacion del usuario
            setCameraPosition(lastLocation);
        }else {
            //Si esta vacio entonces le decimos que me agregue la ubicacion del motor
            locationEngine.addLocationEngineListener(this);
        }

    }
    @SuppressWarnings("MissingPermission")
    private void initializeLocationLayer(){
        //Mostramos una ubicacion al usuario
        locationLayerPlugin = new LocationLayerPlugin(mapView,map,locationEngine);
        //Asignamos la ubicacion habilitada a locationLayerPlugin
        locationLayerPlugin.setLocationLayerEnabled(true);
        //Le decimos a la camara que haga tracking a la posicion
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        //Luego le digo que renderice nuestro localLayerPlugin
        locationLayerPlugin.setRenderMode(RenderMode.COMPASS); //puede ser RenderMode.NORMAL
    }

    //Mover la camara con nuestra ubicacion
    private void setCameraPosition(Location location){
        //llamamos la camara del mapa y la animamos
        //pasandole los parametros latitud, longitud, zoom
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()),13.0));

    }

    //Metodo a implementar despues de colocar el implement OnMapClickListenes

    @Override
    public void onMapClick(@NonNull LatLng point) {

        //Aqui decimos que si existe un marcador elimine el otro marcador existente
        if(destinationMarker !=null){
            map.removeMarker(destinationMarker);
        }
        //Cuando le doy click en la pantalla agrego un marcador de destino
        destinationMarker = map.addMarker(new MarkerOptions().position(point));
        //Establecemos la posicion de destino
        destinationPosition = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        originPosition = Point.fromLngLat(originLocation.getLongitude(),originLocation.getLatitude());

        //Llamamos nuestra funcion getRoute para que nos trace la ruta
        getRoute(originPosition, destinationPosition);

        //Despues de establecer el punto de destino habilito el botonStart
        startButton.setEnabled(true);
        //Le asigno un color azul oscuro
        startButton.setBackgroundResource(R.color.mapboxblue);


    }

    //Funcion para conseguir la ruta de nuestro destino
    private void getRoute(Point origin, Point destination){
        //Decimos que construimos nuestra ruta a trasar
        NavigationRoute.builder()
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null){
                            Log.e(TAG,"No se han encontrado rutas, verifica el usuario y el token de acceso");
                            return;
                        }else if(response.body().routes().size()==0){
                            Log.e(TAG,"Rutas no encontradas");
                            return;
                        }

                        DirectionsRoute currentRoute = response.body().routes().get(0);

                        if(navigationMapRoute != null){
                            navigationMapRoute.removeRoute();
                        }else {
                            //Incializar nuestra ruta del mapa de navegacion que hicimos la variable
                            //para el comienzo navigationMapRoute
                            navigationMapRoute = new NavigationMapRoute(null,mapView,map);
                        }


                        //Añado la ruta
                        navigationMapRoute.addRoute(currentRoute);

                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e(TAG, "Error:"+t.getMessage());
                    }
                });
    }


    //################

    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        //solicitamos las actualizaciones de ubicacion
        locationEngine.requestLocationUpdates();

    }

    @Override
    public void onLocationChanged(Location location) {
        //decimos que si la localitacion de la camara no esta vacia
        ////asignele la posicion de la locaclizacion a la camara
        if(location !=null){
            originLocation = location;
            setCameraPosition(location);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //Presente toast o dialog
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            enableLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsManager.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }
    @SuppressWarnings("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        //Le digo qe si la diferente a vacio haga una descarga de la localizacion
        if(locationEngine != null){
            locationEngine.requestLocationUpdates();
        }
        //Le digo que si la capa de localizacion es diferente a vacio
        //inicie la capa de localizacion
        if(locationLayerPlugin !=null){
            locationLayerPlugin.onStart();
        }
        //Le digo que inicie la visualizacion del mapa
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Para evitar fugas de memoria detenemos la ubicacion removemos las descargas
        if(locationEngine !=null){
            locationEngine.removeLocationUpdates();
        }
        if(locationLayerPlugin !=null){
            locationLayerPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Aqui decimos que si el localizacion del motor es diferente a vacio
        ////desactivamos la ubicacion del motor
        if(locationEngine != null){
            locationEngine.deactivate();
        }
        mapView.onDestroy();
    }




}
