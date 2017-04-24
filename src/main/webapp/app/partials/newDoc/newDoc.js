angular.module('app.newDoc', []);
angular.module('app').requires.push('app.newDoc');
angular.module('app.newDoc')
.config(function config($routeProvider) {
	$routeProvider.when('/newDoc', {
		controller: 'NewDocCtrl',
		templateUrl: 'app/partials/newDoc/newDoc.html'
	}).when('/newDoc/:ID_DB', {
		controller: 'NewDocCtrl',
		templateUrl: 'app/partials/newDoc/newDoc.html'
	}).when('/newDoc/:ID_DB/:ID', {
		controller: 'NewDocCtrl',
		templateUrl: 'app/partials/newDoc/newDoc.html'
	});
})
.controller('NewDocCtrl', function($rootScope,$scope,$http,$location,$route,lupaFactory,$sce,$routeParams,
		eduOverlayFactory,dataFactoryApp,paginationFactory,factoryCRUD,factoryPagCrud) {
	
	//compruebo que tengo el rol requerido para dicha pantalla
	if (!$rootScope.checkAnyPerfil(appConfig.adminProfile)) return;
	
	var apiViewModelos = dataFactoryApp(appConfig.urlBase+'/VIEW_MODELOS/:id', '');
	var apiNewDoc = dataFactoryApp(appConfig.urlBaseApp+'/instanciarModelo/:dbid/:unid', '');
	var apiViewModeloSeccion = dataFactoryApp(appConfig.urlBase+'/VIEW_MODELO_SECCION/:id', '');

	$scope.params = $routeParams;
	$scope.dataModelo = $routeParams;
	$scope.dataSecciones = [];
	$scope.aditionalDeps = {};
	$scope.dependencias={};
	$scope.fileName='';
	
	function cargaModelo(){
		apiViewModelos.get({id:$scope.dataModelo.ID},function(data){
			$scope.dataModelo=data;
			$scope.fileName=data.FILENAME;
		}, $rootScope.overlay.errorManager);
		
		var opts ={
				limit:100,
				orderby:'ORDEN',
				order:'ASC',
				offset: 0,
				filter: '[ID_DB]=='+$scope.dataModelo.ID_DB+' AND [ID_MODELO]=='+$scope.dataModelo.ID,
				fields:'*'				
		};
		
		apiViewModeloSeccion.getAll(opts,function(data){
			//recorro las secciones para ver qué dependencias tienen, de momento cada dependencia es un imputbox y ya esta
			console.log(data);
			$scope.dataSecciones=data;
			$scope.dependencias=$scope.aditionalDeps;
			//construir el array de dependencias e iniciarlo con los valores prefijados en la url
			for(var i=0;i<data.length;i++){
				arr = data[i].DEPENDENCIAS.split(',');
				
				for(var j=0;j<arr.length;j++){
				
				var key = arr[j];
				var valor = $scope.params[key];
				if (valor==undefined) valor='';
				$scope.dependencias[key]=valor;
					
				/*var estaba=false;
				for(var j=0;j<$scope.dependencias.length;j++){
					if ($scope.dependencias[j].KEY==key) estaba=true;
				}
				if (!estaba) {
					if ($scope.params[key]!=undefined) $scope.dependencias.push({KEY:key, VALOR:$scope.params[key]});
					else $scope.dependencias.push({KEY:key});
				}*/
				}
			}
			console.log($scope.dependencias);
			
		}, $rootScope.overlay.errorManager);
	}

	$scope.$watch('user.attr',function(){
		if (!$rootScope.user || !$rootScope.user.attr) return;
		if ($rootScope.user.attr.DATABASE) $scope.databaseSel = $rootScope.user.attr.DATABASE;
	});	

	var ID=$routeParams.ID_DB;
	//LUPAS
	var lupaModelos={
			title:'Seleccione un documento',
			url:appConfig.urlBase+'/VIEW_MODELOS/:id',
			orderby:'FILENAME',
			filter: '[ID_DB]=='+ID,
			fields:'DB_FILENAME,FILENAME',
			size:'lg'
	}
	$scope.openLupaModelos=function(){
		lupaFactory().open(lupaModelos,function(data){
			$scope.dataModelo=data;
			cargaModelo();
		},function(data){
		});
	}
	
	function crearDocumento(data){
		var objData = {
				dep:$scope.dependencias,
				data:data
		}
		var obj = {
			dbid:$scope.dataModelo.ID_DB,
			unid:$scope.dataModelo.ID,
			name:$scope.fileName
		}
		console.log(objData);
		apiNewDoc.execute(obj,objData,function(data){
			console.log('###################DOCUMENTO');
			console.log(data);
			var url = '/documentos/'+data.ID_DB+'/'+data.ID;
			//aqui se pasan los parametros del servicio a la otra pantalla
			//url: url de vuelta a la aplicacion llamante
			if ($scope.params.url!=undefined) {
				url+='?url='+encodeURIComponent($scope.params.url)
				if ($scope.params.url!=undefined) url+='?url='+encodeURIComponent($scope.params.url)
			}
			$location.url(url);
		}, $rootScope.overlay.errorManager);
	}
	
	$scope.crear = function(){
		//urlServiceData: url que permite obtener los datos del servicio
		//urlServiceData: url que provee de los datos
		//urlServiceDataMethod: metodo: GET(default), POST
		//NOTA: el objeto deps con sus aditionalDeps se pasa como urlParamas o body a la llamada urlServiceData con el metodo urlServiceDataMethod
		//antes de invocar la creacion del documento
		if ($scope.params.urlServiceData!=undefined){
			var url = $scope.params.urlServiceData;
			var method = $scope.params.urlServiceDataMethod;
			if (method==undefined) method = 'GET';
			$http({
				method: method,
				url: url,
				params: $scope.dependencias,
				data: $scope.dependencias,
				withCredentials: true,
				responseType: 'json'
			}).then(function successCallback(response) {
				$scope.data=response.data;
				//crearDocumento(response.data);
			}, function errorCallback(response) {
				$rootScope.overlay.errorMessage(response.statusText+' : '+response.data);
			});
		} else {
			$scope.data={};
			//crearDocumento({});
		}
	}
	
	$scope.crearDocumento = function(){
		crearDocumento($scope.data);
	}
	
	/*parametros admitidos aqui:
	 * ID_DB: identificador de la base de datos
	 * ID: identificador del modelo (opcional, abre lupa de seleccion de modelo si no se indica)
	 * url: url de back despues de crear el documento
	 * urlServiceData: url que provee de los datos
	 * urlServiceDataMethod: metodo: GET(default), POST
	 * aditionalDeps: lista separada por comas de dependencias adicionales que el usuario debe rellenar
	 * 
	 * NOTA: el objeto deps con sus aditionalDeps se pasa como urlParamas o body a la llamada urlServiceData con el metodo urlServiceDataMethod
	 * antes de invocar la creacion del documento
	 * También se pueden preinstanciar el objeto deps con datos en la url
	 */
	console.log('DATA MODELO');
	console.log($scope.params);
	console.log('termino');
	//aditionalDependences y asignar valores
	if ($scope.params.aditionalDeps!=undefined){
		var deps = $scope.params.aditionalDeps.split(',');
		for(var i=0;i<deps.length;i++){
			var key = deps[i];
			var valor = $scope.params[key];
			console.log(valor);
			if (valor==undefined) valor='';
			$scope.aditionalDeps[key]=valor;
			/*
			if ($scope.params[key]!=undefined) $scope.aditionalDeps.push({KEY:key, VALOR:$scope.params[key]});
			else $scope.aditionalDeps.push({KEY:key});*/
		}
	}
	//asignar valores a las dependencias en funcion de los valores de la url en params
	//NOTA ver dentro de la seleccion del modelo
	
	
	//ID_DB y ID
	if ($scope.params.ID_DB!=undefined) {
		if ($scope.params.ID!=undefined) cargaModelo();
		else $scope.openLupaModelos();
	} else {
		$scope.overlay.errorMessage('No se ha seleccionado una base de datos');
		return;
	}
	
});

