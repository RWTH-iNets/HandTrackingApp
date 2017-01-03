import sys, math, pygame, time
import numpy as np
import quaternion
from operator import itemgetter

import sauron.io
from sauron.logevent import RotationVectorEvent
from sauron.vector3 import Vector3
from sauron.quaternion_math import rotate


class Cuboid:
    def __init__(self, width, height, depth):
        self.vertices = [
            Vector3(-depth/2,  width/2, -height/2),
            Vector3( depth/2,  width/2, -height/2),
            Vector3( depth/2, -width/2, -height/2),
            Vector3(-depth/2, -width/2, -height/2),
            Vector3(-depth/2,  width/2,  height/2),
            Vector3( depth/2,  width/2,  height/2),
            Vector3( depth/2, -width/2,  height/2),
            Vector3(-depth/2, -width/2,  height/2)
        ]

        self.faces  = [
            (0, 1, 2, 3),
            (1, 5, 6, 2),
            (5, 4, 7, 6),
            (4, 0, 3, 7),
            (0, 4, 5, 1),
            (3, 2, 6, 7)
        ]

        self.colors = [
            (255,   0, 255),
            (255,   0,   0),
            (  0, 255,   0),
            (  0,   0, 255),
            (  0, 255, 255),
            (255, 255,   0)
        ]


class Simulation:
    def __init__(self, win_width = 640, win_height = 480):
        # Initialize pygame
        pygame.init()
        self.screen = pygame.display.set_mode((win_width, win_height))
        pygame.display.set_caption("Mobile device orientation visualization")
        self.clock = pygame.time.Clock()

        # Open database
        self.database = sauron.io.load('LoggingService.db');
        self.log_session = self.database.get_session(self.database.get_all_session_ids()[0])

        # Create our model using the dimensions of a LG G4 phone (in cm)
        self.model = Cuboid(7.61, 14.89, 0.63)

    def project_point(self, v, fov, viewer_distance):
        """ Transforms this 3D point to 2D using a perspective projection. """
        factor = fov / (viewer_distance + v.x)
        z = v.z * factor + self.screen.get_width() / 2
        y = -v.y * factor + self.screen.get_height() / 2
        return Vector3(z, y, v.x)

    def run(self):
        starttime = time.clock();

        while True:
            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    pygame.quit()
                    sys.exit()

            self.clock.tick(50)
            self.screen.fill((0,32,0))

            # Transform and project vertices
            t = [self.project_point(rotate(self.get_current_model_transform(time.clock() - starttime), v), 256, 40) for v in self.model.vertices]

            # Calculate the average Z values of each face.
            avg_z = []
            i = 0
            for f in self.model.faces:
                z = (t[f[0]].z + t[f[1]].z + t[f[2]].z + t[f[3]].z) / 4.0
                avg_z.append([i,z])
                i = i + 1

            # Draw the faces using the Painter's algorithm:
            # Distant faces are drawn before the closer ones.
            for tmp in sorted(avg_z,key=itemgetter(1),reverse=True):
                face_index = tmp[0]
                f = self.model.faces[face_index]

                pointlist = [(t[f[0]].x, t[f[0]].y), (t[f[1]].x, t[f[1]].y),
                             (t[f[1]].x, t[f[1]].y), (t[f[2]].x, t[f[2]].y),
                             (t[f[2]].x, t[f[2]].y), (t[f[3]].x, t[f[3]].y),
                             (t[f[3]].x, t[f[3]].y), (t[f[0]].x, t[f[0]].y)]

                pygame.draw.polygon(self.screen, self.model.colors[face_index], pointlist)
                           

            pygame.display.flip()

    def get_current_model_transform(self, simulation_time):
        return self.log_session.events[self.log_session.get_nearest_event_index(simulation_time, RotationVectorEvent)].quaternion           


if __name__ == "__main__":
    Simulation().run()