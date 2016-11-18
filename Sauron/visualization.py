import sys, math, pygame, math3d, time
from operator import itemgetter

import sauron.io
from sauron.logevent import RotationVectorEvent


class Cuboid:
    def __init__(self, width, height, depth):
        self.vertices = [
            math3d.Vector(-depth/2,width/2,-height/2),
            math3d.Vector(depth/2,width/2,-height/2),
            math3d.Vector(depth/2,-width/2,-height/2),
            math3d.Vector(-depth/2,-width/2,-height/2),
            math3d.Vector(-depth/2,width/2,height/2),
            math3d.Vector(depth/2,width/2,height/2),
            math3d.Vector(depth/2,-width/2,height/2),
            math3d.Vector(-depth/2,-width/2,height/2)
        ]
        self.faces  = [
            (0,1,2,3),
            (1,5,6,2),
            (5,4,7,6),
            (4,0,3,7),
            (0,4,5,1),
            (3,2,6,7)
        ]
        self.colors = [
            (255,0,255),
            (255,0,0),
            (0,255,0),
            (0,0,255),
            (0,255,255),
            (255,255,0)
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
        return math3d.Vector(z, y, v.x)

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
            t = [self.project_point(self.get_current_model_transform(time.clock() - starttime) * v, 256, 4) for v in self.model.vertices]

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
        prev_rotvec_event_idx = None
        for idx, log_event in enumerate(self.log_session.events):
            if isinstance(log_event, RotationVectorEvent):
                if log_event.session_time > simulation_time:
                    return (log_event if prev_rotvec_event_idx is None else self.log_session.events[prev_rotvec_event_idx]).quaternion
                prev_rotvec_event_idx = idx
        return self.log_session.events[prev_rotvec_event_idx].quaternion if prev_rotvec_event_idx is not None else None


if __name__ == "__main__":
    Simulation().run()