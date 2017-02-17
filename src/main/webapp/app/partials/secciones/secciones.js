angular.module('app.secciones', []);
angular.module('app').requires.push('app.secciones');
angular.module('app.secciones')
.config(function config($routeProvider) {
	$routeProvider.when('/secciones', {
		controller: 'SeccionesCtrl',
		templateUrl: 'app/partials/secciones/secciones.html'
	});
})
.controller('SeccionesCtrl', function($rootScope,$scope,$http,$location,$route,$sce,
		eduOverlayFactory,dataFactoryApp,paginationFactory,factoryCRUD,factoryPagCrud) {
	
	//compruebo que tengo el rol requerido para dicha pantalla
	if (!$rootScope.checkAnyPerfil(appConfig.adminProfile)) return;
	
	var apiAttNames = dataFactoryApp(appConfig.urlBaseAttach+'/getAllAttachmentsNames/:dbid/:id', '');
	var apiSeccion = dataFactoryApp(appConfig.urlBase+'/SECCION/:id', '');
	
	$scope.apiApp = dataFactoryApp(appConfig.urlBase+'/VIEW_SECCIONES/:id', '');
	$scope.pagination = paginationFactory();
	$scope.apiCrud = factoryCRUD($scope.apiApp, $rootScope.overlay);
	$scope.apiPagCrudFn = factoryPagCrud(1, $scope.apiCrud, $scope.pagination);
	$scope.data=[];
	$scope.dataExample=[];
	$scope.dataSel={};
	$scope.databaseSel=undefined;
	$scope.showCategoria=false;
	$scope.insert=false;

	// GESTION DE ARCHIVOS
	$scope.uoptions={
		url:appConfig.urlBaseAttach+'/addAttachment',
		alias:'attach',
		//filters:['pdf'],
		autoUpload:false,
		removeAfterUpload: true,
		queueLimit: 1,
		withCredentials: true,
		formData: [{ dbid:'',
			unid:'',
			attach:''
		}]
	};
	
	$scope.attOptions={
		table: 'SECCION',
		dbid:'',
		unid:''
	};


	
	//GESTION DEL LISTADO
	$scope.genOptions = {
			limit:10,
			orderby:'ID',
			order:'ASC',
			offset: 0,
			filter: '',
			fields:'*'
		};
	
	function mostrarDatos() {
		$rootScope.overlay.showOverlay();
		$scope.apiPagCrudFn.setOptions($scope.genOptions);
		$scope.apiPagCrudFn.getPage();
	}
	$scope.$on('pageLoaded', function(event, data) {
		$rootScope.overlay.hideOverlay();
		if (data.id==1) $scope.data = data.data;
	});
	$scope.ver = function(item) {
		console.log(item);
		$scope.dataSel=item;
		$scope.showCategoria=true;
		$scope.insert=false;
		//actualizo el uploader con los ids
		$scope.refreshAttachments();
	}
	$scope.volver = function() {
		$scope.dataSel={};
		$scope.showCategoria=false;
		mostrarDatos();
		//no cambio de hoja
	}
	$scope.sortSrc = function(param){
		$scope.apiPagCrudFn.setSort(param);
	}
	$scope.setPageSrc = function() {
		$rootScope.overlay.showOverlay();
		$scope.apiPagCrudFn.setPage();
	}
	
	$scope.newItem=function(){
		if ($scope.databaseSel==undefined){
			$scope.overlay.errorMessage('No se ha seleccionado una base de datos');
			return;
		}

		$scope.dataSel={};
		$scope.dataSel.ID_DB=$scope.databaseSel.ID;
		$scope.dataSel.DB_FILENAME=$scope.databaseSel.FILENAME;
		$scope.showCategoria=true;
		$scope.insert=true;
	}
	
	$scope.grabar = function () {
		$rootScope.overlay.showOverlay();

		if ($scope.insert){
			apiSeccion.insert($scope.dataSel, function(data){
				$rootScope.overlay.hideOverlay();
				$scope.insert=false;
				$scope.dataSel=data;
				$scope.refreshAttachments();
			}, $rootScope.overlay.errorManager);
		} else {
			apiSeccion.update({id:$scope.dataSel.ID}, $scope.dataSel, function(data){
				$rootScope.overlay.hideOverlay();
				$scope.insert=false;
				$scope.dataSel=data;
				$scope.refreshAttachments();
			}, $rootScope.overlay.errorManager);
		};
		
	}
	
	
	$scope.eliminar = function (ID) {
		$scope.overlay.okCancelMessage('¿Realmente quiere eliminar este elemento?',function(){
			$rootScope.overlay.showOverlay();
			apiSeccion.remove({id:ID}, function(data){
				$rootScope.overlay.hideOverlay();
				mostrarDatos();
			}, $rootScope.overlay.errorManager);
		});
	}

	$scope.$watch('user.attr',function(){
		if (!$rootScope.user || !$rootScope.user.attr) return;
		
		if ($rootScope.user.attr.DATABASE) $scope.databaseSel = $rootScope.user.attr.DATABASE;
	});	

	$scope.refreshAttachments = function(){
		console.log("#########");
		var dbid=$scope.dataSel.ID_DB;
		var id =$scope.dataSel.ID;

		$scope.uoptions.formData[0].dbid=dbid;
		$scope.uoptions.formData[0].unid=id;

		$scope.attOptions.dbid=dbid;
		$scope.attOptions.unid=id;
	
		//cargo los adjuntos del registro
		apiAttNames.getAll({dbid:dbid,id:id},function(data){
			$scope.dataSel.attNames=data;
			console.log(data);
			$scope.refreshClobs();
		}, $rootScope.overlay.errorManager);
	}

	$scope.refreshClobs = function(){
		console.log("#########");
		var id =$scope.dataSel.ID;
		apiSeccion.get({id:id},function(data){
			$scope.TXT=data.TXT;
			$scope.HTML=$sce.trustAsHtml(data.HTML);
			console.log(data);
		}, $rootScope.overlay.errorManager);
	}
	
	
	mostrarDatos();
	
});

