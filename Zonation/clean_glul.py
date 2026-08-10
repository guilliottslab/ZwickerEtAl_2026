import os
import numpy as np
from skimage.morphology import remove_small_objects
from scipy.ndimage import binary_fill_holes
from skimage import io
from PIL import Image
Image.MAX_IMAGE_PIXELS = None
from scipy.ndimage import binary_erosion, binary_closing

base_dir = '/home/michielvc/mountfs3/2025/Christian Zwicker_UCSc/2025_09_24_ChristianZ_Axioscan/'
im_files = os.listdir(base_dir)
wdir = '/srv/data/michielvc/data/other_projects/christian/final_data/segments/'
crap_dir = '/srv/data/michielvc/data/other_projects/christian/final_data/segments/crap/'
im_ids = [24868, 24869, 24870, 24871, 24876, 24877, 24878, 24879, 24880, 24881, 24882]

d_s = {
    24878: 'scene0',
    24877: 'scene0',
    24876: 'scene1', #meh
    24882: 'scene1',
    24881: 'scene0',
    24880: 'scene0',
    24879: 'scene1',
    24871: 'scene0',
    24870: 'scene1',
    24868: 'scene1',
    24869: 'scene1' #meh
}
# read tissue, glul and holes
for file_name in im_ids:
    print(file_name)
    scene_oi = d_s[file_name][-1]
    glul = io.imread(os.path.join(wdir, f'glul/{file_name}_scene{scene_oi}.png'))
    glul = remove_small_objects(glul, min_size=5000)
    tissue = io.imread(os.path.join(wdir, f'tissue/{file_name}_scene{scene_oi}.png')).astype(bool)
    veins = io.imread(os.path.join(wdir, f'veins/{file_name}_scene{scene_oi}.png'))

    # some basic morpholical operations to clean up the glul mask
    slc = glul.astype(bool)
    slc = binary_closing(slc, structure=np.ones((10*4,10*4)))
    slc = remove_small_objects(binary_fill_holes(slc), min_size=6000)
    eroded_slc = binary_erosion(slc, structure=np.ones((20*4,20*4)))
    io.imsave(os.path.join(f'/srv/data/michielvc/data/other_projects/christian/final_data/segments/augmented_glul/{file_name}_scene{scene_oi}.png'), eroded_slc.astype(np.uint8)*255)

