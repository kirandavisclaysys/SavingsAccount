'use strict';

const http = require('http');

// https://www.npmjs.com/package/express
// "Fast, unopinionated, minimalist web framework for node"
const express = require('express');

// https://www.npmjs.com/package/body-parser
// "Node.js body parsing middleware."
const bodyParser = require('body-parser');

const httpProxy = require('http-proxy');

// https://www.npmjs.com/package/morgan
// "HTTP request logger middleware for node.js"
const morgan = require('morgan');

// for file watching and reading
const fs = require('fs');
require('log-timestamp');

// filename of the parameters
const paramsFile = './connector-request-params.json';

var configParams = {};
var tileSendsOnPort = 8083;
var connectorListensOnPort = 8082;
var proxyRunsOnPort = 9000;


const addOrUpdateParam = (collection, item) => {
  if (collection && item) {
    let match = collection.find((c) => c.name == item.name);
    if (match) {
      match.value = item.value;
    }
    else {
      collection.push(item);
    }
  }
}

const addOrUpdateUserData = (userData, item) => {
  if (userData && item) {
    for (var prop in item) {
      userData[prop] = item[prop];
    }
  }
}

//
// config file watcher/parser for operating parameters
//

const readParams = () => {
  console.log(`====================== Reading config from ${paramsFile} ======================`);
  var data=fs.readFileSync(paramsFile, 'utf8');
  configParams = JSON.parse(data);
  console.log('====================== configParams ======================');
  console.log(configParams);

}

// read parameter file on startup
readParams();

fs.watchFile(paramsFile, (curr, prev) => {
  console.log(`====================== ${paramsFile} file Changed ======================`);
  readParams();
});


//
// create the proxy server
//

const proxy = httpProxy.createProxyServer({});

// Restream parsed body before proxying
proxy.on('proxyReq', function(proxyReq, req, res, options) {
  console.log('====================== proxying ======================');
  console.log(req.url);
  if(req.body) {
    let bodyData = JSON.stringify(req.body);
    proxyReq.setHeader('Content-Type','application/json');
    proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
    // Stream the content
    proxyReq.write(bodyData);
  }
});


// handler for tile-to-connector requests
const proxyApp = express();
proxyApp.use(bodyParser.json());
proxyApp.use(bodyParser.urlencoded({extended: true}));
proxyApp.use(function(req, res){
    console.log('====================== ORIGINAL REQUEST ======================');
    console.log(req.body);

    if (req.body.connectorParametersResponse 
      && req.body.connectorParametersResponse.parameters
      && req.body.connectorParametersResponse.parameters.valuePair) {

        if (configParams.connectorParameters) {
          configParams.connectorParameters.forEach((p) => {
            addOrUpdateParam(req.body.connectorParametersResponse.parameters.valuePair, p);
          });
        }
        console.log('====================== PARAMS ======================');
        console.log(req.body.connectorParametersResponse.parameters.valuePair);

    }

    if (configParams.userData && req.body.externalServicePayload
      && req.body.externalServicePayload.userData) {
        addOrUpdateUserData(req.body.externalServicePayload.userData, configParams.userData);
        console.log('====================== USERDATA ======================');
        console.log(req.body.externalServicePayload.userData);
    }
    proxy.web(req, res, {
      target: 'http://127.0.0.1:' + connectorListensOnPort
    })
  });

http.createServer(proxyApp).listen(tileSendsOnPort, '0.0.0.0', () => {
  console.log('Tile listener on ', tileSendsOnPort);
});



// 
// Listen for tile requests
//

const app = express();
app.use(morgan('dev'));
app.use(bodyParser.json());

app.post('/*', (req, res) => { 
  console.log('proxy app.post:', req.url);
  res.send('POST received');
});

app.get('/*', (req, res) => { 
  res.send('GET received');
});

http.createServer(app).listen(proxyRunsOnPort, '127.0.0.1', () => {
  console.log('Applicaton server on', proxyRunsOnPort);
});
