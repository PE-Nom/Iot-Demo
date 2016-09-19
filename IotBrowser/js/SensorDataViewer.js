var SensorDataViewer = (function(){

  // コンストラクタ
  var SensorDataViewer = function(svg_name)
  {

    this.svg_name = svg_name;
    this.points = new Array();

    this.d3Line = d3.svg.line()
    //　　Scaleは自動でX軸の目盛表示を変換してくれるが、グラフ描画のX座標までは
    //   考慮されないので配列のインデックスの10倍を表示上のX座標としている。
    //    .x(function(d) { return d.num; })
      .x(function(d,i){return i * 10})
    //  乱数で生成する　value　は　0.0～1.0　の間にある。
    //  X軸と同じく、Y軸も Scale は自動でX軸の目盛表示は変換してくれるがグラフ描画の
    //  Y軸の値までは考慮されない。Y軸値は正規化して表示させる。
    //    .y(function(d) { return d.value; })
      .y(function(d) { return height*d.value; })
    //  interprateすると、負の値が出現し、グラフをはみだしてしまうのでやめる。
    //    .interpolate("cardinal")
      ;

    this.xScale = d3.scale.linear()
          .domain([0,width/10])
          .range([0,width]);

    this.yScale = d3.scale.linear()
          .domain([-1,1])
          .range([height,0]);

    this.samplingCount = 0;

  }

  // prototypeの宣言
  var p = SensorDataViewer.prototype;

  // 初期化
  p.create_viewer = function(){
    width = $("#"+this.svg_name).width() - margin.left - margin.right;
    this.stage = d3.select("#"+this.svg_name)
              .append("svg")
                .attr("id",this.svg_name)
                .attr("width", width + margin.left + margin.right)
                .attr("height", height + margin.top + margin.bottom)
              .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
    this.initializeAxis();
    this.updateAxis();
  }

  // ウィンドウリサイズ時の再計算
  p.resize_viewer = function(){

    d3.select("svg#"+this.svg_name).remove();
    width = $("#"+this.svg_name).width() - margin.left - margin.right;
    this.stage = d3.select("#"+this.svg_name)
                  .append("svg")
                    .attr("id",this.svg_name)
                    .attr("width", width + margin.left + margin.right)
                    .attr("height", height + margin.top + margin.bottom)
                  .append("g")
                    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    while(this.points.length > parseInt(width/10)) {
      // 配列先頭（最古）のデータを削除
      this.points.shift();
    }
    this.getScales();
    this.initializeAxis();
    this.updateAxis();

    // グラフ再描画
    this.stage.append("path")
      .attr("d", this.d3Line(this.points))
      .attr("id",this.svg_name)
      .attr("fill", "none")
      .attr("opacity", 1);

  }

  //　データ更新と再描画
  p.update = function(count,data) {
      // 新たなデータを乱数で生成して配列末尾（最新)に追加
      var datum = {
//        num : this.samplingCount++,
//        value : Math.random()
        num : count,
        value : (1.0-data)/2
      };
      var x = d3.max(this.points,function(d){ return d.num });
      if( parseInt(x) > parseInt(datum.num)){
        this.points = [];
      }
      this.points.push(datum);

      // 点数が描画幅を超えたら
      if(this.points.length > parseInt(width/10)) {
          // 配列先頭（最古）のデータを削除
          this.points.shift();
      }
      // 目盛の再計算
      this.getScales();
      this.initializeAxis();

      // 削除する
      this.stage.selectAll("path").remove();
      this.stage.selectAll("g").remove();

      // XY軸更新
      this.updateAxis();

      // グラフ再描画
      this.stage.append("path")
        .attr("d", this.d3Line(this.points))
        .attr("id",this.svg_name)
        .attr("fill", "none")
        .attr("opacity", 1);

//      if(this.samplingCount%20 == 0){
//        this.updatelabel();
//      }
    }

    // スケール更新
    p.getScales = function (){
/*
      if(this.points.lenght != 0){
        this.yScale = d3.scale.linear()
                  .domain([d3.min(this.points,function(d){ return d.value }),
                           d3.max(this.points,function(d){ return d.value })])
                  .range([height,0]);
      }
*/

//    var min = d3.min(this.points,function(d){ return d.num });
      var min = 0;
      if(this.points.length != 0){
        min = parseInt(this.points[0].num);
        for(var i=0;i<this.points.length; i++){
          if(parseInt(this.points[i].num) < min){
            min = parseInt(this.points[i].num);
          }
        }
      }

      var xmin = parseFloat(min);
      var xwidth = parseFloat(width/10);
      var xmax = xmin+xwidth;
//        console.log("Xmin : " + xmin + ",Xmax :" + xmax + ", xwidth : " + xwidth);
      this.xScale = d3.scale.linear()
            .domain([xmin,xmax])
            .range([0,width]);
    }

    // XY軸目盛の初期化
    p.initializeAxis = function(){
      this.xAxis = d3.svg.axis()
                    .scale(this.xScale)
                    .orient("bottom")
                    .innerTickSize(-height)
                    .outerTickSize(5)
                    .tickPadding(10);

      this.xAxisT = d3.svg.axis()
                    .scale(this.xScale)
                    .orient("top")
                    .outerTickSize(5)
                    .tickPadding(10);

      this.yAxis = d3.svg.axis()
                    .scale(this.yScale)
                    .orient("left")
                    .innerTickSize(-width)
                    .outerTickSize(5)
                    .tickPadding(10);

      this.yAxisR = d3.svg.axis()
                    .scale(this.yScale)
                    .orient("right")
                    .outerTickSize(5)
                    .tickPadding(10);
    }

    // XY軸目盛の更新
    p.updateAxis = function(){
      // 目盛再描画
      this.stage.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(this.xAxis)
              .append("text")
                .attr("class", "xlabel")
                .attr("text-anchor", "end")
                .attr("x", width)
                .text("測定番号")
                  .attr("id",this.svg_name);

      this.stage.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0,0)")
            .call(this.xAxisT);

      this.stage.append("g")
            .attr("class", "y axis")
            .call(this.yAxis)
              .append("text")
                .attr("class", "ylabel")
                .attr("text-anchor", "end")
                .attr("y", 6)
                .attr("dy", ".75em")
                .attr("transform", "rotate(-90)")
                .text("測定値")
                  .attr("id",this.svg_name);

      this.stage.append("g")
            .attr("class", "y axis")
            .attr("transform", "translate(" + width + " ,0)")
            .call(this.yAxisR)

    }

    // 以下はイベントリスナの実装
    p.updateMeasureStatus = function(sw){
      var measureStatusElement = document.getElementById(this.svg_name+"-measure-status");
      if(sw=="STARTED"){
        measureStatusElement.style.background = "darkblue";
        measureStatusElement.style.color = "white";
        measureStatusElement.value = "計測";
      }
      else{
        measureStatusElement.style.background = "gold";
        measureStatusElement.style.color = "black";
        measureStatusElement.value = "停止";
      }
    }

    p.updateLoadStatus = function(sw){
      var loadStatusElement = document.getElementById(this.svg_name+"-load-status");

      if(sw=="LOADED"){
        loadStatusElement.style.background = "darkgreen";
        loadStatusElement.style.color = "white";
        loadStatusElement.value = "稼働";
      }
      else{
        loadStatusElement.style.background = "gold";
        loadStatusElement.style.color = "black";
        loadStatusElement.value = "非稼働";
      }
    }

    p.updateMeasureBtn = function(caption){
      document.getElementById(this.svg_name + "-measure-cmd").innerText = caption;
    }

/*
    // 操作ボタンのイベントリスナ
    p.readstatus = function(){
      console.log("readStatus @ "+this.svg_name);
    }

    p.measurement = function(elementid){
      var cmdMeasureElement = document.getElementById(elementid);
      if(cmdMeasureElement.innerText == "START"){
        this.updateMeasureBtn("STOP");
        console.log("stop @ "+this.svg_name);
      }
      else{
        this.updateMeasureBtn("START");
        console.log("start @ "+this.svg_name);
      }
    }
*/
    return SensorDataViewer;

})();
