<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.12.2" name="portao" tilewidth="64" tileheight="48" tilecount="2" columns="2">
 <editorsettings>
  <export target="portao.tsx" format="tsx"/>
 </editorsettings>
 <image source="portao.png" width="128" height="48"/>
 <tile id="0">
  <properties>
   <property name="acao" value="abrir_portao"/>
   <property name="colisao" type="bool" value="true"/>
   <property name="isActive" type="bool" value="true"/>
   <property name="isInteractive" type="bool" value="true"/>
   <property name="isTransparent" type="bool" value="false"/>
   <property name="type" value="map_object"/>
  </properties>
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="42.1023" width="64" height="2.87551"/>
  </objectgroup>
 </tile>
 <tile id="1">
  <properties>
   <property name="acao" value="null"/>
   <property name="colisao" type="bool" value="true"/>
   <property name="isActive" type="bool" value="true"/>
   <property name="isInteractive" type="bool" value="false"/>
   <property name="isTransparent" type="bool" value="false"/>
   <property name="type" value="map_object"/>
  </properties>
  <objectgroup draworder="index" id="3">
   <object id="3" x="14.0435" y="42">
    <polygon points="0,0 0.0434783,-3 -14,-0.0434783 -13.9565,3.17391"/>
   </object>
   <object id="7" x="50.0273" y="42.0323">
    <polygon points="0,0 -0.0434783,-3 14,-0.0434783 13.9565,3.17391"/>
   </object>
  </objectgroup>
 </tile>
</tileset>
