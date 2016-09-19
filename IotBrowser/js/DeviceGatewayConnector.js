function SigV4Utils(){}

SigV4Utils.sign = function(key, msg) {
  var hash = CryptoJS.HmacSHA256(msg, key);
  return hash.toString(CryptoJS.enc.Hex);
};

SigV4Utils.sha256 = function(msg) {
  var hash = CryptoJS.SHA256(msg);
  return hash.toString(CryptoJS.enc.Hex);
};

SigV4Utils.getSignatureKey = function(key, dateStamp, regionName, serviceName) {
  var kDate = CryptoJS.HmacSHA256(dateStamp, 'AWS4' + key);
  var kRegion = CryptoJS.HmacSHA256(regionName, kDate);
  var kService = CryptoJS.HmacSHA256(serviceName, kRegion);
  var kSigning = CryptoJS.HmacSHA256('aws4_request', kService);
  return kSigning;
};

// Class Defnition : DeviceGatewayConnector
var DeviceGatewayConnector = (function(){

  // コンストラクタ
  var DeviceGatewayConnector = function(credentialInfo){
    this.regionName = credentialInfo.regionName;           //  AWS Region name
    this.identityPoolId = credentialInfo.identityPoolId;   //　Cognito Identity pool ID
    this.awsIotEndpoint = credentialInfo.awsIotEndpoint;   //  AWS Iot DeviceGateway endopoint name
  }

  // prototypeの宣言
  var p = DeviceGatewayConnector.prototype;

  // Credentialの取得
  p.getCredentials = function() {
    AWS.config.region = this.regionName;
    var self =this; // Closureに自己保持
    var cognitoidentity = new AWS.CognitoIdentity();
    var params = {
      IdentityPoolId: this.identityPoolId
    };
    cognitoidentity.getId(params, function(err, objectHavingIdentityId) {
      if (err) return self.createWssEndpoint(err);
      cognitoidentity.getCredentialsForIdentity(objectHavingIdentityId, function(err, data) {
        if (err) return self.createWssEndpoint(err);
        var credentials = {
          accessKey: data.Credentials.AccessKeyId,
          secretKey: data.Credentials.SecretKey,
          sessionToken: data.Credentials.SessionToken
        };
        console.log('CognitoIdentity has provided temporary credentials successfully.');
        self.createWssEndpoint(null, credentials);
      });
    });
  }

  // Wss Endpointの生成
  p.createWssEndpoint = function(err,creds){

    if (err) return this.createClient(err);

    var time = moment.utc();
    var dateStamp = time.format('YYYYMMDD');
    var amzdate = dateStamp + 'T' + time.format('HHmmss') + 'Z';
    var service = 'iotdevicegateway';
    var region = this.regionName;
    var secretKey = creds.secretKey;
    var accessKey = creds.accessKey;
    var algorithm = 'AWS4-HMAC-SHA256';
    var method = 'GET';
    var canonicalUri = '/mqtt';
    var host = this.awsIotEndpoint;
    var sessionToken = creds.sessionToken;

    var credentialScope = dateStamp + '/' + region + '/' + service + '/' + 'aws4_request';
    var canonicalQuerystring = 'X-Amz-Algorithm=AWS4-HMAC-SHA256';
    canonicalQuerystring += '&X-Amz-Credential=' + encodeURIComponent(accessKey + '/' + credentialScope);
    canonicalQuerystring += '&X-Amz-Date=' + amzdate;
    canonicalQuerystring += '&X-Amz-SignedHeaders=host';

    var canonicalHeaders = 'host:' + host + '\n';
    var payloadHash = SigV4Utils.sha256('');
    var canonicalRequest = method + '\n' + canonicalUri + '\n' + canonicalQuerystring + '\n' + canonicalHeaders + '\nhost\n' + payloadHash;

    var stringToSign = algorithm + '\n' +  amzdate + '\n' +  credentialScope + '\n' +  SigV4Utils.sha256(canonicalRequest);
    var signingKey = SigV4Utils.getSignatureKey(secretKey, dateStamp, region, service);
    var signature = SigV4Utils.sign(signingKey, stringToSign);

    canonicalQuerystring += '&X-Amz-Signature=' + signature;

    var wssEndpoint = 'wss://' + host + canonicalUri + '?' + canonicalQuerystring;
    wssEndpoint += '&X-Amz-Security-Token=' + encodeURIComponent(sessionToken);
    console.log("wssEndpoint : "+wssEndpoint);
    this.createClient(null, wssEndpoint);

  }

  p.createClient = function(err, endpoint) {
    if (err) {
      console.log('failed', err);
      return;
    }
    var clientId = Math.random().toString(36).substring(7);
    var self = this;
    this.client = new Paho.MQTT.Client(endpoint, clientId);
    var connectOptions = {
        useSSL: true,
        timeout: 3,
        mqttVersion: 4,
        onSuccess: this.subscribe
    };
    this.client.connect(connectOptions);
    this.client.onMessageArrived = this.onMessage;
    this.client.onConnectionLost = function(e) { console.log(e) };
  }

  p.subscribe = function(){
    subscribe();
  }

  p.addSubscribeTopic = function(topic){
    this.client.subscribe(topic);
    console.log("add subscribe topic : " + topic);
  }

  p.onMessage = function(message){
    subscribeListener(message);
  }

  p.createConnector = function (sub,listener){
    this.sub = sub;
    this.listener = listener;
    this.getCredentials();
  }
  p.publish = function(topic,content) {
    var message = new Paho.MQTT.Message(content);
    message.destinationName = topic;
    this.client.send(message);
    console.log("sent");
  }

  return DeviceGatewayConnector;
})();
