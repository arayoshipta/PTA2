#PTA2
## About
PTA2 is a ImageJ1.x plugins that enable automatic particle tracking. This is a new version of [PTA](https://github.com/arayoshipta/projectPTAj). New features are as follows:

1. Point detection can be performed by "Find Maxima"
2. Four different methods for localization, "Find Maxima", "Centroid", "Center of Mass", and "2D Gaussian".
3. No need to use JNI (Java Native Library)
4. Simple edit tool for tracks (delete, split, and concatenate)
5. Compatible with ImageJ macro (enables batch processing)

## Important
This plugin is free to use, but the copyright is not abandoned. **I HAVE NO RESPONSIBILITY TO ANY DAMAGE BECAUSE OF THE USE OF THIS PLUG-IN**


##Install
If you are beginner, I recommend you to use [Fiji](https://fiji.sc/) since it equips all required libraries for PTA2.
### Fiji
 1. Put "PTA2_-0.6.5-SNAPSHOT.jar" into Fiji's plugins folder
 2. Start Fiji, then you'll find "PTA2" in plugins menu bar

### ImageJ
 1. Download additional jar files as follows
 - JFreeChart.jar
 - JCommons.jar
	 - These can be downloads from [JFreeChart](http://www.jfree.org/jfreechart/)
 - commons-math3-3.x.x.jar
	 - [Apache Commons Math](http://commons.apache.org/proper/commons-math/download_math.cgi)

## Usage
### Multiple tracking
1. Prepare stack image.
2. Launch **PTA2** from plugin menu.
3. Adjust threshold. *This process is required if you want to store **Size** information.*
4. Adjust **Tol.**. This parameter is completely same mean as "Tolerance" in "Find Maxima" method.
5. If you check **Tracking parameters** in Main Window (***Intensity, Size, Angle, Circularity***), at the linkage steps, these parameters are also included to calculate the distance.
	- distance is calculated as follows: d = xy-loc + root(intensity) + root(size) + root(angle) + root(circularity)
5. Press **Preview** button to check whether particles are detected.
6. Press **Multiple Track** button to perform multiple tracking. If the dimension of images are not frame but stack, you will be ask whether you want to convert the image from slice to frame.


### Table
1. After processing, table will open automatically. 
2. When you click the row of table,  multi-plot graph will be appeared.
3. Multiple selection enables multiple indication of Roi's in the image.
4. Table has several menus, Save, Edit, Analyze

#### Save menu
1. You can save your data as text data from save menu.
	 - I encourage you to analyze data by Pandas in python. 

#### Edit menu
1. You can **delete**, **split**, and **concatenate** tracks.
2. **Split** can be performed at the current frame
3. To concatenate tracks, choose two tracks that don't overlap about time.
4. If there is a gap between last frame of first track and first frame of second track, simple interpolation will be performed.

#### Analyze menu
1. **Scatter plot** enable simple plotting the localization of points. You can change the scale. Instead of using mean intensity, you can use "1" value.
	- This may be useful for counting molecules.
2. **Show multi-Z intensities** enable the obtaining intensity trajectory at first frame of ROI. The data are exported as Result table
	- This function can be used to analyze on/off or blinking event of particles.

### Use with macro
Because PTA2 has **PTA2Dialog** you can call this from ImageJ macro like this;

```
run("PTA2Dialog", "methods=Centroid tol=40 roisize=12 search=3 intensity size angle circularity filename=/Users/hoge/Documents/SampleMovie/ save");;
```
By this macro, all tracked data 

**filename** indicates where you want to save.
**save** enables save multi-Z intensities at fixed locations

#### History
- 2016.07.25 version 0.5 uploaded to GitHub
- 2017.02.01 version 0.6.5
	- fixed the problem of velocity and area calculation

#### Author information
Yoshiyuki Arai

E-mail: projectptaj@gmail.com
